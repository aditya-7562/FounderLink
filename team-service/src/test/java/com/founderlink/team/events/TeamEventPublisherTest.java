package com.founderlink.team.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TeamEventPublisher teamEventPublisher;

    private static final String EXCHANGE = "founderlink.exchange";
    private static final String INVITE_ROUTING_KEY = "team.invite.sent";
    private static final String ACCEPTED_ROUTING_KEY = "team.member.accepted";
    private static final String REJECTED_ROUTING_KEY = "team.member.rejected";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(teamEventPublisher, "exchange", EXCHANGE);
        ReflectionTestUtils.setField(teamEventPublisher, "teamInviteRoutingKey", INVITE_ROUTING_KEY);
        ReflectionTestUtils.setField(teamEventPublisher, "teamAcceptedRoutingKey", ACCEPTED_ROUTING_KEY);
        ReflectionTestUtils.setField(teamEventPublisher, "teamRejectedRoutingKey", REJECTED_ROUTING_KEY);
    }

    @Test
    @DisplayName("publishTeamInviteEvent - publishes event successfully")
    void publishTeamInviteEvent_Success() {
        TeamInviteEvent event = new TeamInviteEvent(1L, 100L, "CTO");

        teamEventPublisher.publishTeamInviteEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(EXCHANGE),
                eq(INVITE_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    @DisplayName("publishTeamMemberAcceptedEvent - publishes event successfully")
    void publishTeamMemberAcceptedEvent_Success() {
        TeamMemberAcceptedEvent event = new TeamMemberAcceptedEvent(
                1L, 101L, 5L, 300L, "CTO"
        );

        teamEventPublisher.publishTeamMemberAcceptedEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(EXCHANGE),
                eq(ACCEPTED_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    @DisplayName("publishTeamMemberRejectedEvent - publishes event successfully")
    void publishTeamMemberRejectedEvent_Success() {
        TeamMemberRejectedEvent event = new TeamMemberRejectedEvent(
                1L, 101L, 5L, 300L, "CTO"
        );

        teamEventPublisher.publishTeamMemberRejectedEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(EXCHANGE),
                eq(REJECTED_ROUTING_KEY),
                eq(event)
        );
    }

    @Test
    @DisplayName("publishTeamInviteEvent - handles exception gracefully")
    void publishTeamInviteEvent_HandlesException() {
        TeamInviteEvent event = new TeamInviteEvent(1L, 100L, "CTO");
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(INVITE_ROUTING_KEY), eq(event));

        teamEventPublisher.publishTeamInviteEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(EXCHANGE), eq(INVITE_ROUTING_KEY), eq(event));
    }

    @Test
    @DisplayName("publishTeamMemberAcceptedEvent - handles exception gracefully")
    void publishTeamMemberAcceptedEvent_HandlesException() {
        TeamMemberAcceptedEvent event = new TeamMemberAcceptedEvent(
                1L, 101L, 5L, 300L, "CTO"
        );
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ACCEPTED_ROUTING_KEY), eq(event));

        teamEventPublisher.publishTeamMemberAcceptedEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(EXCHANGE), eq(ACCEPTED_ROUTING_KEY), eq(event));
    }

    @Test
    @DisplayName("publishTeamMemberRejectedEvent - handles exception gracefully")
    void publishTeamMemberRejectedEvent_HandlesException() {
        TeamMemberRejectedEvent event = new TeamMemberRejectedEvent(
                1L, 101L, 5L, 300L, "CTO"
        );
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(REJECTED_ROUTING_KEY), eq(event));

        teamEventPublisher.publishTeamMemberRejectedEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(eq(EXCHANGE), eq(REJECTED_ROUTING_KEY), eq(event));
    }
}
