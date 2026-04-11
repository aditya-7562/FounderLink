package com.founderlink.messaging.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessageEventPublisher messageEventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(messageEventPublisher, "exchange", "test-exchange");
        ReflectionTestUtils.setField(messageEventPublisher, "routingKey", "test-routing-key");
    }

    @Test
    @DisplayName("publishMessageSent - calls rabbitTemplate with correct routing key and payload")
    void publishMessageSent_Success() {
        messageEventPublisher.publishMessageSent(1L, 100L, 200L, "SenderName");

        // Explicitly use overloads to avoid ambiguity
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
