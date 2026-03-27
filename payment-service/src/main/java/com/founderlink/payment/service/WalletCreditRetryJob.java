package com.founderlink.payment.service;

import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dto.external.WalletDepositRequestDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retries wallet credit for payments that succeeded at Razorpay
 * but failed to credit the startup's wallet (e.g. wallet-service was down).
 *
 * Runs every 60 seconds. Safe to retry because wallet-service deposit
 * is idempotent (checks referenceId before crediting).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WalletCreditRetryJob {

    private final PaymentRepository paymentRepository;
    private final WalletServiceClient walletServiceClient;

    @Scheduled(fixedDelay = 60_000)
    public void retryFailedWalletCredits() {
        List<Payment> uncredited = paymentRepository
                .findByStatusAndWalletCredited(PaymentStatus.SUCCESS, false);

        if (uncredited.isEmpty()) {
            return;
        }

        log.info("Found {} payments with pending wallet credits", uncredited.size());

        for (Payment payment : uncredited) {
            retrySingle(payment);
        }
    }

    @Transactional
    public void retrySingle(Payment payment) {
        try {
            log.info("Retrying wallet credit - paymentId: {}, startupId: {}, amount: {}",
                    payment.getId(), payment.getStartupId(), payment.getAmount());

            walletServiceClient.createWallet(payment.getStartupId());

            WalletDepositRequestDto depositRequest = new WalletDepositRequestDto();
            depositRequest.setReferenceId(payment.getInvestmentId());
            depositRequest.setStartupId(payment.getStartupId());
            depositRequest.setAmount(payment.getAmount());
            depositRequest.setSourcePaymentId(payment.getId());
            depositRequest.setIdempotencyKey("wallet-deposit-" + payment.getInvestmentId());

            walletServiceClient.depositFunds(depositRequest);

            payment.setWalletCredited(true);
            paymentRepository.save(payment);

            log.info("Wallet credit retry SUCCESS - paymentId: {}", payment.getId());

        } catch (Exception e) {
            log.error("Wallet credit retry FAILED - paymentId: {}, error: {}. Will retry next cycle.",
                    payment.getId(), e.getMessage());
        }
    }
}
