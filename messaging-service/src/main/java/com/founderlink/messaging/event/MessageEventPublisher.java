package com.founderlink.messaging.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MessageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    public MessageEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishMessageSent(Long messageId, Long senderId, Long receiverId, String senderName) {
        Map<String, Object> event = Map.of(
                "messageId", messageId,
                "senderId", senderId,
                "receiverId", receiverId,
                "senderName", senderName
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Published MESSAGE_SENT event: sender={}, receiver={}", senderId, receiverId);
    }
}
