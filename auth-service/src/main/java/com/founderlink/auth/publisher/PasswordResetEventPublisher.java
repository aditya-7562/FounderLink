package com.founderlink.auth.publisher;

import com.founderlink.auth.config.RabbitMQConfig;
import com.founderlink.auth.dto.PasswordResetEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPasswordResetEvent(PasswordResetEmailEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.PASSWORD_RESET_ROUTING_KEY,
                    event
            );
            log.info("Published password reset event for email: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish password reset event for email: {}", event.getEmail(), e);
        }
    }
}
