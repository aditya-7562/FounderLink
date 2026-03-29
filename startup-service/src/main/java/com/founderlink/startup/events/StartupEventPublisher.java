package com.founderlink.startup.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.startup.routing-key}")
    private String startupRoutingKey;

    @Value("${rabbitmq.startup.deleted.routing-key}")
    private String startupDeletedRoutingKey;

    public void publishStartupCreatedEvent(
            StartupCreatedEvent event) {
        try {
            log.info("Publishing STARTUP_CREATED " +
                    "event for startupId: {}",
                    event.getStartupId());

            rabbitTemplate.convertAndSend(
                    exchange,
                    startupRoutingKey,
                    event);

            log.info("STARTUP_CREATED published!!!");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "STARTUP_CREATED: {}",
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
                    exchange,
                    startupDeletedRoutingKey,
                    event);

            log.info("STARTUP_DELETED published!!!");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "STARTUP_DELETED: {}",
                    e.getMessage());
        }
    }
}