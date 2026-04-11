package com.founderlink.investment.events;

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
class InvestmentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private InvestmentEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "exchange", "test-exchange");
        ReflectionTestUtils.setField(publisher, "investmentRoutingKey", "created-key");
        ReflectionTestUtils.setField(publisher, "investmentApprovedRoutingKey", "approved-key");
        ReflectionTestUtils.setField(publisher, "investmentRejectedRoutingKey", "rejected-key");
    }

    @Test
    void publishInvestmentCreatedEvent_Success() {
        InvestmentCreatedEvent event = new InvestmentCreatedEvent(1L, 101L, 202L, 5L, new BigDecimal("1000.00"));
        publisher.publishInvestmentCreatedEvent(event);
        verify(rabbitTemplate).convertAndSend(eq("test-exchange"), eq("created-key"), eq(event));
    }

    @Test
    void publishInvestmentCreatedEvent_Error() {
        InvestmentCreatedEvent event = new InvestmentCreatedEvent();
        doThrow(new RuntimeException("Rabbit error")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThatThrownBy(() -> publisher.publishInvestmentCreatedEvent(event))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishInvestmentApprovedEvent_Success() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent(1L, 101L, 202L, 5L, new BigDecimal("1000.00"));
        publisher.publishInvestmentApprovedEvent(event);
        verify(rabbitTemplate).convertAndSend(eq("test-exchange"), eq("approved-key"), eq(event));
    }

    @Test
    void publishInvestmentApprovedEvent_Error() {
        InvestmentApprovedEvent event = new InvestmentApprovedEvent();
        doThrow(new RuntimeException("Rabbit error")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThatThrownBy(() -> publisher.publishInvestmentApprovedEvent(event))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishInvestmentRejectedEvent_Success() {
        InvestmentRejectedEvent event = new InvestmentRejectedEvent(1L, 101L, 202L, 5L, new BigDecimal("1000.00"), "Reason");
        publisher.publishInvestmentRejectedEvent(event);
        verify(rabbitTemplate).convertAndSend(eq("test-exchange"), eq("rejected-key"), eq(event));
    }

    @Test
    void publishInvestmentRejectedEvent_Error() {
        InvestmentRejectedEvent event = new InvestmentRejectedEvent();
        doThrow(new RuntimeException("Rabbit error")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThatThrownBy(() -> publisher.publishInvestmentRejectedEvent(event))
                .isInstanceOf(IllegalStateException.class);
    }
}
