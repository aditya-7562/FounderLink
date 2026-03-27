package com.founderlink.payment.service;

import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dto.external.WalletDepositRequestDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletCreditRetryJobTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WalletServiceClient walletServiceClient;

    @InjectMocks
    private WalletCreditRetryJob walletCreditRetryJob;

    @Test
    void retryFailedWalletCredits_Success() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStartupId(100L);
        payment.setInvestmentId(200L);
        payment.setAmount(BigDecimal.valueOf(500));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setWalletCredited(false);

        when(paymentRepository.findByStatusAndWalletCredited(PaymentStatus.SUCCESS, false))
                .thenReturn(List.of(payment));

        walletCreditRetryJob.retryFailedWalletCredits();

        verify(walletServiceClient, times(1)).createWallet(100L);
        verify(walletServiceClient, times(1)).depositFunds(any(WalletDepositRequestDto.class));
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void retryFailedWalletCredits_ExceptionDoesNotStopExecution() {
        Payment payment1 = new Payment();
        payment1.setId(1L);
        payment1.setStartupId(100L);

        Payment payment2 = new Payment();
        payment2.setId(2L);
        payment2.setStartupId(200L);

        when(paymentRepository.findByStatusAndWalletCredited(PaymentStatus.SUCCESS, false))
                .thenReturn(List.of(payment1, payment2));

        when(walletServiceClient.createWallet(100L)).thenThrow(new RuntimeException("Wallet Service Down"));
        // payment 2 succeeds
        lenient().when(walletServiceClient.createWallet(200L)).thenReturn(null);

        walletCreditRetryJob.retryFailedWalletCredits();

        verify(walletServiceClient, times(1)).createWallet(100L);
        verify(walletServiceClient, times(1)).createWallet(200L);
        verify(paymentRepository, times(1)).save(payment2);
        verify(paymentRepository, never()).save(payment1);
    }

    @Test
    void retryFailedWalletCredits_NoUncreditedPayments() {
        when(paymentRepository.findByStatusAndWalletCredited(PaymentStatus.SUCCESS, false))
                .thenReturn(Collections.emptyList());

        walletCreditRetryJob.retryFailedWalletCredits();

        verify(walletServiceClient, never()).createWallet(anyLong());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
