package com.founderlink.investment.events;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.investment.service.InvestmentService;

@ExtendWith(MockitoExtension.class)
class PaymentResultEventConsumerTest {

    @Mock
    private InvestmentService investmentService;

    @InjectMocks
    private PaymentResultEventConsumer consumer;

    @Test
    void handlePaymentCompleted() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 500L);
        consumer.handlePaymentCompleted(event);
        verify(investmentService).markCompletedFromPayment(1L);
    }

    @Test
    void handlePaymentFailed() {
        PaymentFailedEvent event = new PaymentFailedEvent(1L, 500L, "Insufficient funds");
        consumer.handlePaymentFailed(event);
        verify(investmentService).markPaymentFailedFromPayment(1L);
    }
}
