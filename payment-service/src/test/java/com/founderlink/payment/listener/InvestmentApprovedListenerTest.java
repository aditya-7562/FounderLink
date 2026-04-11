package com.founderlink.payment.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.event.InvestmentApprovedEvent;
import com.founderlink.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class InvestmentApprovedListenerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private InvestmentApprovedListener listener;

    @Test
    void handleInvestmentApproved_NewPayment_CreatesSuccessfully() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent(
                1L, 100L, 200L, 300L, BigDecimal.valueOf(500));
        
        when(paymentRepository.findByInvestmentId(1L)).thenReturn(Optional.empty());

        listener.handleInvestmentApproved(event);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void handleInvestmentApproved_Duplicate_SkipsCreation() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent(
                1L, 100L, 200L, 300L, BigDecimal.valueOf(500));
        
        when(paymentRepository.findByInvestmentId(1L)).thenReturn(Optional.of(new Payment()));

        listener.handleInvestmentApproved(event);

        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
