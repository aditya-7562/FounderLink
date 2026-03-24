package com.founderlink.investment.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvestmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.investment.routing-key}")
    private String investmentRoutingKey;

    public void publishInvestmentCreatedEvent(
            InvestmentCreatedEvent event) {
        try {
            log.info("Publishing INVESTMENT_CREATED " +
                    "event for startupId: {}",
                    event.getStartupId());

            rabbitTemplate.convertAndSend(
                    exchange,
                    investmentRoutingKey,
                    event);

            log.info("INVESTMENT_CREATED published!!!!");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "INVESTMENT_CREATED: {}",
                    e.getMessage());
        }
    }
}