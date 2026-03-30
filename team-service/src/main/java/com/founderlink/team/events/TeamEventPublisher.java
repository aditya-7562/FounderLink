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

    @Value("${rabbitmq.team.invite.routing-key}")
    private String teamInviteRoutingKey;

    @Value("${rabbitmq.team.accepted.routing-key}")
    private String teamAcceptedRoutingKey;

    @Value("${rabbitmq.team.rejected.routing-key}")
    private String teamRejectedRoutingKey;

    public void publishTeamInviteEvent(
            TeamInviteEvent event) {
        try {
            log.info("Publishing TEAM_INVITE_SENT " +
                    "event for startupId: {}",
                    event.getStartupId());

            rabbitTemplate.convertAndSend(
                    exchange,
                    teamInviteRoutingKey,
                    event);

            log.info("TEAM_INVITE_SENT published!!!");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "TEAM_INVITE_SENT: {}",
                    e.getMessage());
        }
    }

    public void publishTeamMemberAcceptedEvent(
            TeamMemberAcceptedEvent event) {
        try {
            log.info("Publishing TEAM_MEMBER_ACCEPTED " +
                    "event for startupId: {}",
                    event.getStartupId());

            rabbitTemplate.convertAndSend(
                    exchange,
                    teamAcceptedRoutingKey,
                    event);

            log.info("TEAM_MEMBER_ACCEPTED published!!!");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "TEAM_MEMBER_ACCEPTED: {}",
                    e.getMessage());
        }
    }

    public void publishTeamMemberRejectedEvent(
            TeamMemberRejectedEvent event) {
        try {
            log.info("Publishing TEAM_MEMBER_REJECTED " +
                    "event for startupId: {}",
                    event.getStartupId());

            rabbitTemplate.convertAndSend(
                    exchange,
                    teamRejectedRoutingKey,
                    event);

            log.info("TEAM_MEMBER_REJECTED published!!!");

        } catch (Exception e) {
            log.error("Failed to publish " +
                    "TEAM_MEMBER_REJECTED: {}",
                    e.getMessage());
        }
    }
}