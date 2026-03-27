package com.founderlink.payment.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.payment.completed.routing-key}")
    private String paymentCompletedRoutingKey;

    @Value("${rabbitmq.payment.failed.routing-key}")
    private String paymentFailedRoutingKey;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, paymentCompletedRoutingKey, event);
            log.info("PAYMENT_COMPLETED published for investmentId={}", event.getInvestmentId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish PAYMENT_COMPLETED event", e);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, paymentFailedRoutingKey, event);
            log.info("PAYMENT_FAILED published for investmentId={}", event.getInvestmentId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish PAYMENT_FAILED event", e);
        }
    }
}
