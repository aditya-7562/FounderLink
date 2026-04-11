package com.founderlink.auth.publisher;

import com.founderlink.auth.config.RabbitMQConfig;
import com.founderlink.auth.dto.PasswordResetEmailEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PasswordResetEventPublisher publisher;

    private PasswordResetEmailEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new PasswordResetEmailEvent("test@founderlink.com", "Test User", "123456");
    }

    @Test
    void publishPasswordResetEventShouldSendToRabbitMQSuccessfully() {
        doNothing().when(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FOUNDERLINK_EXCHANGE),
                eq(RabbitMQConfig.PASSWORD_RESET_ROUTING_KEY),
                eq(testEvent)
        );

        publisher.publishPasswordResetEvent(testEvent);

        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitMQConfig.FOUNDERLINK_EXCHANGE,
                RabbitMQConfig.PASSWORD_RESET_ROUTING_KEY,
                testEvent
        );
    }

    @Test
    void publishPasswordResetEventShouldThrowIllegalStateExceptionWhenRabbitMQFails() {
        doThrow(new RuntimeException("RabbitMQ Connection Failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatThrownBy(() -> publisher.publishPasswordResetEvent(testEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Failed to publish password reset event");

        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitMQConfig.FOUNDERLINK_EXCHANGE,
                RabbitMQConfig.PASSWORD_RESET_ROUTING_KEY,
                testEvent
        );
    }
}
