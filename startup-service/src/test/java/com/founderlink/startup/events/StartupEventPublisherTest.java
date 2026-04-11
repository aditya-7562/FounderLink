package com.founderlink.startup.events;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
class StartupEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private StartupEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventPublisher, "exchange", "test.exchange");
        ReflectionTestUtils.setField(eventPublisher, "startupRoutingKey", "startup.created");
        ReflectionTestUtils.setField(eventPublisher, "startupDeletedRoutingKey", "startup.deleted");
    }

    @Test
    void publishStartupCreatedEvent_sendsToRabbit() {
        StartupCreatedEvent event = new StartupCreatedEvent(1L, "EduReach", 42L, "EdTech", new BigDecimal("5000000"));

        eventPublisher.publishStartupCreatedEvent(event);

        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("startup.created"),
                eq(event)
        );
    }

    @Test
    void publishStartupDeletedEvent_sendsToRabbit() {
        StartupDeletedEvent event = new StartupDeletedEvent(1L, 42L);

        eventPublisher.publishStartupDeletedEvent(event);

        verify(rabbitTemplate).convertAndSend(
                eq("test.exchange"),
                eq("startup.deleted"),
                eq(event)
        );
    }

    @Test
    void publishStartupCreatedEvent_throwsIllegalStateOnRabbitFailure() {
        StartupCreatedEvent event = new StartupCreatedEvent(1L, "EduReach", 42L, "EdTech", new BigDecimal("5000000"));
        doThrow(new RuntimeException("Broker down")).when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertThatThrownBy(() -> eventPublisher.publishStartupCreatedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to publish STARTUP_CREATED")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void publishStartupDeletedEvent_throwsIllegalStateOnRabbitFailure() {
        StartupDeletedEvent event = new StartupDeletedEvent(1L, 42L);
        doThrow(new RuntimeException("Broker down")).when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertThatThrownBy(() -> eventPublisher.publishStartupDeletedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to publish STARTUP_DELETED")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
