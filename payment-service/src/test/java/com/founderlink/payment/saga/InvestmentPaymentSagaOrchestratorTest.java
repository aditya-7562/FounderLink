package com.founderlink.payment.saga;

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
import com.founderlink.payment.event.InvestmentRejectedEvent;
import com.founderlink.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class InvestmentPaymentSagaOrchestratorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private InvestmentPaymentSagaOrchestrator orchestrator;

    @Test
    void handleInvestmentRejected_PaymentNotFound_DoesNothing() {
        Long investmentId = 1L;
        InvestmentRejectedEvent event = new InvestmentRejectedEvent(investmentId, 100L, 200L, 300L, BigDecimal.TEN, "Bad credit score");
        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.empty());

        orchestrator.handleInvestmentRejected(event);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleInvestmentRejected_PaymentPending_MarksFailed() {
        Long investmentId = 1L;
        InvestmentRejectedEvent event = new InvestmentRejectedEvent(investmentId, 100L, 200L, 300L, BigDecimal.TEN, "Bad credit score");
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setStatus(PaymentStatus.PENDING);
        
        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));

        orchestrator.handleInvestmentRejected(event);

        verify(paymentRepository).save(payment);
        assert(payment.getStatus() == PaymentStatus.FAILED);
        assert(payment.getFailureReason().contains("Bad credit score"));
    }

    @Test
    void handleInvestmentRejected_PaymentSuccessful_DoesNotChange() {
        Long investmentId = 1L;
        InvestmentRejectedEvent event = new InvestmentRejectedEvent(investmentId, 100L, 200L, 300L, BigDecimal.TEN, "Rejected late");
        Payment payment = new Payment();
        payment.setId(100L);
        payment.setStatus(PaymentStatus.SUCCESS);
        
        when(paymentRepository.findByInvestmentId(investmentId)).thenReturn(Optional.of(payment));

        orchestrator.handleInvestmentRejected(event);

        verify(paymentRepository, never()).save(any());
    }
}
