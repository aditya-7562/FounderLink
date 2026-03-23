package com.founderlink.team.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.team.routing-key}")
    private String teamRoutingKey;

    public void publishTeamInviteEvent(
            TeamInviteEvent event) {
        try {
            log.info("Publishing TEAM_INVITE_SENT " +
                    "event for startupId: {}",
                    event.getStartupId());

            rabbitTemplate.convertAndSend(
                    exchange,
                    teamRoutingKey,
                    event);

            log.info("TEAM_INVITE_SENT published!!!");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "TEAM_INVITE_SENT: {}",
                    e.getMessage());
        }
    }
}