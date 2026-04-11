package com.founderlink.auth.publisher;

import com.founderlink.auth.config.RabbitMQConfig;
import com.founderlink.auth.dto.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserRegisteredEventPublisher publisher;

    private UserRegisteredEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new UserRegisteredEvent(1L, "test@founderlink.com", "Test User", "FOUNDER");
    }

    @Test
    void publishUserRegisteredEventShouldSendToRabbitMQSuccessfully() {
        doNothing().when(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.FOUNDERLINK_EXCHANGE),
                eq(RabbitMQConfig.USER_REGISTERED_ROUTING_KEY),
                eq(testEvent)
        );

        publisher.publishUserRegisteredEvent(testEvent);

        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitMQConfig.FOUNDERLINK_EXCHANGE,
                RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                testEvent
        );
    }

    @Test
    void publishUserRegisteredEventShouldCatchExceptionWhenRabbitMQFails() {
        doThrow(new RuntimeException("RabbitMQ Connection Failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> publisher.publishUserRegisteredEvent(testEvent))
                .doesNotThrowAnyException();
                
        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitMQConfig.FOUNDERLINK_EXCHANGE,
                RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                testEvent
        );
    }
}
