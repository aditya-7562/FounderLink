package com.founderlink.payment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.founderlink.payment.client.PaymentGatewayClient;
import com.founderlink.payment.dto.request.PaymentHoldRequestDto;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.entity.PaymentTransactionLog;
import com.founderlink.payment.exception.IdempotencyException;
import com.founderlink.payment.exception.PaymentGatewayException;
import com.founderlink.payment.exception.PaymentNotFoundException;
import com.founderlink.payment.idempotency.IdempotencyService;
import com.founderlink.payment.mapper.PaymentMapper;
import com.founderlink.payment.repository.PaymentRepository;
import com.founderlink.payment.repository.PaymentTransactionLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionLogRepository transactionLogRepository;
    private final PaymentGatewayClient paymentGateway;
    private final PaymentMapper paymentMapper;
    private final IdempotencyService idempotencyService;

    // TTL for idempotency keys: 24 hours (prevents replay attacks after 24h)
    private static final long IDEMPOTENCY_KEY_TTL_SECONDS = 24 * 60 * 60;

    @Override
    public PaymentResponseDto holdFunds(PaymentHoldRequestDto holdRequest) {
        log.info("Holding funds for investment {} - amount: ${}, idempotencyKey: {}",
                holdRequest.getInvestmentId(), holdRequest.getAmount(),
                holdRequest.getIdempotencyKey());

        // Step 1: Check Redis cache first (fast path)
        var cachedPaymentId = idempotencyService.getPaymentIdByIdempotencyKey(
                holdRequest.getIdempotencyKey());
        
        if (cachedPaymentId.isPresent()) {
            log.warn("Idempotent request detected (Redis hit) - returning cached payment: {}",
                    cachedPaymentId.get());
            Payment existingPayment = paymentRepository.findById(cachedPaymentId.get())
                    .orElseThrow(() -> new IdempotencyException(
                            "Payment cached but not found in DB: " + cachedPaymentId.get()));
            return paymentMapper.toResponseDto(existingPayment);
        }

        // Step 2: Check database as fallback (for old payments or cache misses)
        var dbPaymentId = paymentRepository.findByIdempotencyKey(holdRequest.getIdempotencyKey());
        if (dbPaymentId.isPresent()) {
            log.warn("Idempotent request detected (DB hit) - returning existing payment: {}",
                    dbPaymentId.get().getId());
            // Cache it for future requests
            idempotencyService.storeIdempotencyKey(
                    holdRequest.getIdempotencyKey(),
                    dbPaymentId.get().getId(),
                    IDEMPOTENCY_KEY_TTL_SECONDS);
            return paymentMapper.toResponseDto(dbPaymentId.get());
        }

        try {
            // Step 3: Call payment gateway to authorize funds
            String authorizationId = paymentGateway.holdFunds(
                    holdRequest.getInvestorId(),
                    holdRequest.getAmount(),
                    holdRequest.getIdempotencyKey(),
                    "Investment hold for startup: " + holdRequest.getStartupId()
            );

            log.info("✓ Hold authorized - auth ID: {}", authorizationId);

            // Create payment record
            Payment payment = new Payment();
            payment.setInvestmentId(holdRequest.getInvestmentId());
            payment.setInvestorId(holdRequest.getInvestorId());
            payment.setStartupId(holdRequest.getStartupId());
            payment.setFounderId(holdRequest.getFounderId());
            payment.setAmount(holdRequest.getAmount());
            payment.setIdempotencyKey(holdRequest.getIdempotencyKey());
            payment.setExternalPaymentId(authorizationId);
            payment.setStatus(PaymentStatus.HELD);

            Payment savedPayment = paymentRepository.save(payment);

            // Log transaction
            PaymentTransactionLog log = new PaymentTransactionLog();
            log.setPayment(savedPayment);
            log.setAction("HOLD_SUCCESS");
            log.setDetails("Authorization ID: " + authorizationId);
            transactionLogRepository.save(log);

            // Step 4: Cache idempotency key in Redis for future requests
            idempotencyService.storeIdempotencyKey(
                    holdRequest.getIdempotencyKey(),
                    savedPayment.getId(),
                    IDEMPOTENCY_KEY_TTL_SECONDS);

            log.info("✓ Payment record created - ID: {}, status: HELD", savedPayment.getId());
            log.info("✓ Idempotency key cached in Redis - TTL: {} hours", 24);

            return paymentMapper.toResponseDto(savedPayment);

        } catch (Exception e) {
            log.error("Failed to hold funds: {}", e.getMessage());
            throw new PaymentGatewayException("Hold funds failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentResponseDto captureFunds(Long paymentId) {
        log.info("Capturing funds for payment ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.HELD) {
            throw new PaymentGatewayException(
                    "Cannot capture payment with status: " + payment.getStatus());
        }

        try {
            // Call payment gateway to capture (charge) funds
            String chargeId = paymentGateway.captureFunds(
                    payment.getExternalPaymentId(),
                    payment.getAmount(),
                    "Investment capture for startup: " + payment.getStartupId()
            );

            log.info("✓ Capture successful - charge ID: {}", chargeId);

            // Update payment
            payment.setExternalPaymentId(chargeId);
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setUpdatedAt(LocalDateTime.now());

            Payment updatedPayment = paymentRepository.save(payment);

            // Log transaction
            PaymentTransactionLog transactionLog = new PaymentTransactionLog();
            transactionLog.setPayment(updatedPayment);
            transactionLog.setAction("CAPTURE_SUCCESS");
            transactionLog.setDetails("Charge ID: " + chargeId);
            transactionLogRepository.save(transactionLog);

            log.info("✓ Payment captured - ID: {}, status: CAPTURED", updatedPayment.getId());

            return paymentMapper.toResponseDto(updatedPayment);

        } catch (Exception e) {
            log.error("Capture failed: {}", e.getMessage());

            // Log failure
            PaymentTransactionLog transactionLog = new PaymentTransactionLog();
            transactionLog.setPayment(payment);
            transactionLog.setAction("CAPTURE_FAILED");
            transactionLog.setDetails("Error: " + e.getMessage());
            transactionLogRepository.save(transactionLog);

            // Mark payment as failed
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            throw new PaymentGatewayException("Capture funds failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentResponseDto releaseFunds(Long paymentId, String reason) {
        log.info("Releasing funds for payment ID: {} - reason: {}", paymentId, reason);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.HELD && 
            payment.getStatus() != PaymentStatus.CAPTURED) {
            log.warn("Cannot release funds with status: {}", payment.getStatus());
            return paymentMapper.toResponseDto(payment);
        }

        try {
            // Call payment gateway to release/void authorization or refund
            paymentGateway.releaseFunds(
                    payment.getExternalPaymentId(),
                    reason
            );

            log.info("✓ Funds released");

            // Update payment
            payment.setStatus(PaymentStatus.RELEASED);
            payment.setUpdatedAt(LocalDateTime.now());

            Payment updatedPayment = paymentRepository.save(payment);

            // Log transaction
            PaymentTransactionLog transactionLog = new PaymentTransactionLog();
            transactionLog.setPayment(updatedPayment);
            transactionLog.setAction("RELEASE_SUCCESS");
            transactionLog.setDetails("Reason: " + reason);
            transactionLogRepository.save(transactionLog);

            log.info("✓ Payment released - ID: {}, status: RELEASED", updatedPayment.getId());

            return paymentMapper.toResponseDto(updatedPayment);

        } catch (Exception e) {
            log.error("Release failed: {}", e.getMessage());

            // Log failure
            PaymentTransactionLog transactionLog = new PaymentTransactionLog();
            transactionLog.setPayment(payment);
            transactionLog.setAction("RELEASE_FAILED");
            transactionLog.setDetails("Error: " + e.getMessage());
            transactionLogRepository.save(transactionLog);

            // Mark payment as failed
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);

            throw new PaymentGatewayException("Release funds failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void markAsTransferred(Long paymentId, String walletTransactionId) {
        log.info("Marking payment {} as transferred - wallet TX: {}", paymentId, walletTransactionId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new PaymentGatewayException(
                    "Cannot mark as transferred with status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.TRANSFERRED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Log transaction
        PaymentTransactionLog transactionLog = new PaymentTransactionLog();
        transactionLog.setPayment(payment);
        transactionLog.setAction("MARK_TRANSFERRED");
        transactionLog.setDetails("Wallet TX: " + walletTransactionId);
        transactionLogRepository.save(transactionLog);

        log.info("✓ Payment marked as transferred - ID: {}", paymentId);
    }

    @Override
    public void markAsFailed(Long paymentId, String failureReason) {
        log.info("Marking payment {} as failed - reason: {}", paymentId, failureReason);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + paymentId));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Log transaction
        PaymentTransactionLog transactionLog = new PaymentTransactionLog();
        transactionLog.setPayment(payment);
        transactionLog.setAction("MARK_FAILED");
        transactionLog.setDetails("Reason: " + failureReason);
        transactionLogRepository.save(transactionLog);

        log.info("✓ Payment marked as failed - ID: {}", paymentId);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + paymentId));
        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByInvestmentId(Long investmentId) {
        Payment payment = paymentRepository.findByInvestmentId(investmentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for investment ID: " + investmentId));
        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean paymentExists(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatus getPaymentStatus(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(Payment::getStatus)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with ID: " + paymentId));
    }
}
