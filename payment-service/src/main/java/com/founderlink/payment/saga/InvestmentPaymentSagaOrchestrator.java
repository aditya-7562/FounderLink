package com.founderlink.payment.saga;

import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dlq.RetryableException;
import com.founderlink.payment.dto.request.PaymentHoldRequestDto;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.dto.external.WalletDepositRequestDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.event.InvestmentApprovedEvent;
import com.founderlink.payment.event.InvestmentCreatedEvent;
import com.founderlink.payment.event.InvestmentRejectedEvent;
import com.founderlink.payment.exception.PaymentNotFoundException;
import com.founderlink.payment.repository.PaymentRepository;
import com.founderlink.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Saga orchestrator for investment payment workflow.
 *
 * This orchestrator coordinates the payment saga across three events:
 * 1. Investment Created → Hold funds (authorize without capture)
 * 2. Investment Approved → Capture funds + Deposit to wallet
 * 3. Investment Rejected → Release held funds (refund)
 *
 * Uses compensating transactions for failure handling.
 */
@Component
@Slf4j
public class InvestmentPaymentSagaOrchestrator {

    private final PaymentService paymentService;
    private final WalletServiceClient walletServiceClient;
    private final PaymentRepository paymentRepository;

    public InvestmentPaymentSagaOrchestrator(
            PaymentService paymentService,
            WalletServiceClient walletServiceClient,
            PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.walletServiceClient = walletServiceClient;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Step 1: When investment is created, place a hold on investor's funds.
     * Status: PENDING_HOLD → HELD (funds authorized but not charged)
     *
     * Executes immediately after investment creation to ensure funds availability.
     */
    @RabbitListener(queues = "investment.created.queue")
    @Transactional
    public void handleInvestmentCreated(InvestmentCreatedEvent event) {
        log.info("Saga Start: Investment Created. investmentId={}, amount={}, investor={}, startup={}",
                event.getInvestmentId(), event.getAmount(), event.getInvestorId(), event.getStartupId());

        try {
            // Step 1.1: Create hold request with idempotency key
            PaymentHoldRequestDto holdRequest = new PaymentHoldRequestDto();
            holdRequest.setInvestmentId(event.getInvestmentId());
            holdRequest.setInvestorId(event.getInvestorId());
            holdRequest.setFounderId(event.getFounderId());
            holdRequest.setStartupId(event.getStartupId());
            holdRequest.setAmount(event.getAmount());
            holdRequest.setIdempotencyKey(UUID.randomUUID().toString());

            // Step 1.2: Hold funds (authorize but don't capture)
            // This ensures investor has sufficient balance before proceeding
            PaymentResponseDto holdResponse = paymentService.holdFunds(holdRequest);

            log.info("✓ Saga Step 1 Complete: Funds held. paymentId={}, amount={}",
                    holdResponse.getId(), holdResponse.getAmount());

        } catch (Exception e) {
            // Step 1.3: Log error for manual intervention
            // Payment hold failure is terminal - no compensation possible
            // (Funds were authorized but hold failed, operator intervention needed)
            log.error("✗ Saga Step 1 Failed: Cannot hold funds. investmentId={}, investor={}, amount={}, error={}",
                    event.getInvestmentId(), event.getInvestorId(), event.getAmount(), e.getMessage(), e);

            // TODO: Send alert to operations team - investment cannot proceed
            // TODO: Update investment status to PAYMENT_HOLD_FAILED
        }
    }

    /**
     * Step 2: When investment is approved by founder, capture the held funds and deposit to wallet.
     * This is a compound step with compensation logic:
     * - Capture: HELD → CAPTURED (actually charge the funds)
     * - Deposit: Transfer captured amount to startup wallet
     *
     * If deposit fails, compensation triggers: Release the captured funds (refund to investor)
     */
    @RabbitListener(queues = "investment.approved.queue")
    @Transactional
    public void handleInvestmentApproved(InvestmentApprovedEvent event) {
        log.info("Saga Step 2a: Investment Approved. investmentId={}, amount={}, startup={}",
                event.getInvestmentId(), event.getAmount(), event.getStartupId());

        try {
            // Step 2.1: Find the payment record (created in Step 1)
            Payment payment = paymentRepository.findByInvestmentId(event.getInvestmentId())
                    .orElseThrow(() -> new PaymentNotFoundException(
                            "No payment found for investment " + event.getInvestmentId()));

            log.info("→ Found payment record. paymentId={}, status={}", payment.getId(), payment.getStatus());

            // Step 2.2: Capture the held funds (actually charge the investor)
            PaymentResponseDto captureResponse = paymentService.captureFunds(payment.getId());
            log.info("✓ Saga Step 2.1 Complete: Funds captured. paymentId={}, externalPaymentId={}",
                    captureResponse.getId(), captureResponse.getExternalPaymentId());

            // Step 2.3: Deposit captured amount to startup wallet
            WalletDepositRequestDto depositRequest = new WalletDepositRequestDto();
            depositRequest.setStartupId(event.getStartupId());
            depositRequest.setAmount(event.getAmount());
            depositRequest.setSourcePaymentId(payment.getId());
            depositRequest.setIdempotencyKey(UUID.randomUUID().toString());

            try {
                walletServiceClient.depositFunds(depositRequest);
                log.info("✓ Saga Step 2.2 Complete: Funds deposited to wallet. startupId={}, amount={}",
                        event.getStartupId(), event.getAmount());

                // Success: Update payment to TRANSFERRED status
                payment.setStatus(PaymentStatus.TRANSFERRED);
                paymentRepository.save(payment);
                log.info("✓ Saga Complete: Payment workflow successful. investmentId={}", event.getInvestmentId());

            } catch (Exception walletException) {
                // Step 2.4: Compensation - Deposit failed, release captured funds
                log.warn("✗ Saga Step 2.2 Failed: Wallet deposit error. startupId={}, error={}",
                        event.getStartupId(), walletException.getMessage());

                log.info("→ Executing Compensation: Releasing captured funds. paymentId={}", payment.getId());

                try {
                    // Compensation: Release the funds that were captured
                    paymentService.releaseFunds(payment.getId(), 
                        "Wallet deposit failed - automatic compensation");
                    
                    // Update payment status to RELEASED (failure recovery)
                    // Note: payment status may already be updated by releaseFunds, 
                    // but we ensure it's marked as compensated
                    payment.setStatus(PaymentStatus.RELEASED);
                    paymentRepository.save(payment);

                    log.warn("✓ Compensation Complete: Funds released. investmentId={}, paymentId={}",
                            event.getInvestmentId(), payment.getId());

                    // TODO: Notify investor of rollback and reason (wallet service unavailable)
                    // TODO: Update investment status to APPROVAL_PROCESSING_FAILED

                } catch (Exception releaseException) {
                    // Critical failure: Cannot release funds
                    log.error("✗ Compensation Failed: Cannot release funds. paymentId={}, error={}",
                            payment.getId(), releaseException.getMessage(), releaseException);

                    // TODO: Send CRITICAL alert - investor's funds are held and cannot be released
                    // TODO: Manual operational intervention required
                }

                // Throw retryable exception for transient wallet failures
                if (isTransientFailure(walletException)) {
                    throw new RetryableException(
                        "Wallet service temporarily unavailable - will retry",
                        walletException,
                        "SERVICE",
                        payment.getId()
                    );
                } else {
                    // Non-retryable failure
                    throw new RuntimeException("Wallet deposit failed, funds released (if possible)", walletException);
                }

        } catch (PaymentNotFoundException e) {
            log.error("✗ Saga Step 2 Failed: Payment not found for investment {}. error={}",
                    event.getInvestmentId(), e.getMessage());

            // TODO: Update investment status to PAYMENT_CAPTURE_FAILED
            // TODO: Send alert - no payment record found (data inconsistency)

        } catch (Exception e) {
            log.error("✗ Saga Step 2 Failed: Unexpected error. investmentId={}, error={}",
                    event.getInvestmentId(), e.getMessage(), e);

            // TODO: Send alert - saga execution failed
        }
    }

    /**
     * Step 3: When investment is rejected by founder, release the held funds.
     * Status: HELD → RELEASED (refund to investor)
     *
     * This compensates for the hold placed in Step 1.
     */
    @RabbitListener(queues = "investment.rejected.queue")
    @Transactional
    public void handleInvestmentRejected(InvestmentRejectedEvent event) {
        log.info("Saga Step 3: Investment Rejected. investmentId={}, amount={}, reason='{}', investor={}",
                event.getInvestmentId(), event.getAmount(), event.getRejectionReason(), event.getInvestorId());

        try {
            // Step 3.1: Find the payment record
            Payment payment = paymentRepository.findByInvestmentId(event.getInvestmentId())
                    .orElseThrow(() -> new PaymentNotFoundException(
                            "No payment found for investment " + event.getInvestmentId()));

            log.info("→ Found payment record. paymentId={}, status={}", payment.getId(), payment.getStatus());

            // Step 3.2: Release the held funds (refund to investor)
            PaymentResponseDto releaseResponse = paymentService.releaseFunds(payment.getId(), 
                "Investment rejected by founder - reason: " + event.getRejectionReason());

            log.info("✓ Saga Step 3 Complete: Funds released. paymentId={}, amount={}",
                    releaseResponse.getId(), releaseResponse.getAmount());
            log.info("✓ Saga Compensated: Investment rejected, refund processed. investmentId={}",
                    event.getInvestmentId());

        } catch (PaymentNotFoundException e) {
            log.error("✗ Saga Step 3 Failed: Payment not found for investment {}. error={}",
                    event.getInvestmentId(), e.getMessage());

            // TODO: Update investment status to PAYMENT_RELEASE_FAILED
            // Likely data inconsistency - payment was never created

        } catch (Exception e) {
            log.error("✗ Saga Step 3 Failed: Cannot release funds. investmentId={}, investor={}, error={}",
                    event.getInvestmentId(), event.getInvestorId(), e.getMessage(), e);

            // TODO: Send alert - funds are held and cannot be released
            // Operator intervention needed
        }
    }
}
