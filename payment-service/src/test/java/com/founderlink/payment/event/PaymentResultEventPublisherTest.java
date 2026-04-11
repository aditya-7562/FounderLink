package com.founderlink.payment.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentResultEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentResultEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "exchange", "test-exchange");
        ReflectionTestUtils.setField(publisher, "paymentCompletedRoutingKey", "completed-key");
        ReflectionTestUtils.setField(publisher, "paymentFailedRoutingKey", "failed-key");
    }

    @Test
    void publishPaymentCompleted_Success() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 101L, 202L, 5L, 303L, new BigDecimal("1000.00"));
        publisher.publishPaymentCompleted(event);
        verify(rabbitTemplate).convertAndSend(eq("test-exchange"), eq("completed-key"), eq(event));
    }

    @Test
    void publishPaymentCompleted_Error() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        doThrow(new RuntimeException("Rabbit error")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThatThrownBy(() -> publisher.publishPaymentCompleted(event))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishPaymentFailed_Success() {
        PaymentFailedEvent event = new PaymentFailedEvent(1L, 101L, 202L, 5L, 303L, new BigDecimal("1000.00"), "Reason");
        publisher.publishPaymentFailed(event);
        verify(rabbitTemplate).convertAndSend(eq("test-exchange"), eq("failed-key"), eq(event));
    }

    @Test
    void publishPaymentFailed_Error() {
        PaymentFailedEvent event = new PaymentFailedEvent();
        doThrow(new RuntimeException("Rabbit error")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThatThrownBy(() -> publisher.publishPaymentFailed(event))
                .isInstanceOf(IllegalStateException.class);
    }
}
