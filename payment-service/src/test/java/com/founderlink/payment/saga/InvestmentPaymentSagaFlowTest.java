package com.founderlink.payment.saga;

import com.founderlink.payment.client.WalletServiceClient;
import com.founderlink.payment.dto.external.WalletResponseDto;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.entity.PaymentStatus;
import com.founderlink.payment.event.InvestmentApprovedEvent;
import com.founderlink.payment.event.InvestmentCreatedEvent;
import com.founderlink.payment.event.InvestmentRejectedEvent;
import com.founderlink.payment.event.PaymentCompletedEvent;
import com.founderlink.payment.event.PaymentFailedEvent;
import com.founderlink.payment.event.PaymentResultEventPublisher;
import com.founderlink.payment.repository.PaymentRepository;
import com.founderlink.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentPaymentSagaFlowTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentResultEventPublisher paymentResultEventPublisher;

    @InjectMocks
    private InvestmentPaymentSagaOrchestrator orchestrator;

    @Test
    void flow1CreationTriggersHold() {
        InvestmentCreatedEvent event = new InvestmentCreatedEvent(
                1L, 101L, 201L, 301L, new BigDecimal("500.00"), null);
        when(paymentService.holdFunds(any())).thenReturn(new PaymentResponseDto(
                10L, 1L, 101L, 301L, 201L, new BigDecimal("500.00"),
                PaymentStatus.HELD, "auth_1", null, LocalDateTime.now(), LocalDateTime.now()));

        orchestrator.handleInvestmentCreated(event);

        verify(paymentService, times(1)).holdFunds(any());
    }

    @Test
    void flow2ApprovalPublishesCompletedAfterCaptureAndDeposit() {
        Payment payment = new Payment();
        payment.setId(55L);
        payment.setInvestmentId(2L);
        payment.setStartupId(300L);
        payment.setStatus(PaymentStatus.HELD);
        payment.setAmount(new BigDecimal("800.00"));

        when(paymentRepository.findByInvestmentId(2L)).thenReturn(Optional.of(payment));
        when(walletServiceClient.depositFunds(any())).thenReturn(new WalletResponseDto(
                7L, 300L, new BigDecimal("800.00"), LocalDateTime.now(), LocalDateTime.now()));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.handleInvestmentApproved(new InvestmentApprovedEvent(
                2L, 111L, 222L, 300L, new BigDecimal("800.00")));

        verify(paymentService, times(1)).captureFunds(55L);
        verify(walletServiceClient, times(1)).depositFunds(any());
        verify(paymentResultEventPublisher, times(1)).publishPaymentCompleted(any(PaymentCompletedEvent.class));
        verify(paymentResultEventPublisher, never()).publishPaymentFailed(any(PaymentFailedEvent.class));
        assertEquals(PaymentStatus.TRANSFERRED, payment.getStatus());
    }

    @Test
    void flow3RejectionReleasesFunds() {
        Payment payment = new Payment();
        payment.setId(88L);
        payment.setInvestmentId(3L);
        payment.setStatus(PaymentStatus.HELD);

        when(paymentRepository.findByInvestmentId(3L)).thenReturn(Optional.of(payment));

        orchestrator.handleInvestmentRejected(new InvestmentRejectedEvent(
                3L, 100L, 200L, 400L, new BigDecimal("250.00"), "reject"));

        verify(paymentService, times(1)).releaseFunds(88L, "Investment rejected by founder - reason: reject");
    }

    @Test
    void duplicateApprovalEventRepublishesCompletedWithoutRecapture() {
        Payment payment = new Payment();
        payment.setId(99L);
        payment.setInvestmentId(4L);
        payment.setStatus(PaymentStatus.TRANSFERRED);

        when(paymentRepository.findByInvestmentId(4L)).thenReturn(Optional.of(payment));

        orchestrator.handleInvestmentApproved(new InvestmentApprovedEvent(
                4L, 111L, 222L, 333L, new BigDecimal("150.00")));

        verify(paymentService, never()).captureFunds(any());
        verify(walletServiceClient, never()).depositFunds(any());
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentResultEventPublisher, times(1)).publishPaymentCompleted(eventCaptor.capture());
        assertEquals(4L, eventCaptor.getValue().getInvestmentId());
    }
}
