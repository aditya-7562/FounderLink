package com.founderlink.startup.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.founderlink.startup.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishStartupCreatedEvent(
            StartupCreatedEvent event) {
    	
        try {
            log.info("Publishing STARTUP_CREATED event " +
                            "for startupId: {}",
                    event.getStartupId());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.STARTUP_EXCHANGE,
                    RabbitMQConfig.STARTUP_ROUTING_KEY,
                    event
            );

            log.info("STARTUP_CREATED event " +
                    "published successfully");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "STARTUP_CREATED event: {}",
                    e.getMessage());
        }
    }
    public void publishStartupDeletedEvent(
            StartupDeletedEvent event) {
        try {
            log.info("Publishing STARTUP_DELETED " +
                    "event for startupId: {}",
                    event.getStartupId());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.STARTUP_EXCHANGE,
                    RabbitMQConfig.STARTUP_DELETED_ROUTING_KEY,
                    event
            );

            log.info("STARTUP_DELETED event " +
                    "published successfully");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "STARTUP_DELETED event: {}",
                    e.getMessage());
        }
    }
}