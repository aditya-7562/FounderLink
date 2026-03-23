package com.founderlink.team.events;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.repository.TeamMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupDeletedEventConsumer {

    private final InvitationRepository
            invitationRepository;
    private final TeamMemberRepository
            teamMemberRepository;

    @RabbitListener(
        queues = "${rabbitmq.startup.deleted.queue}")
    public void handleStartupDeletedEvent(
            StartupDeletedEvent event) {

        log.info("Received STARTUP_DELETED event " +
                "for startupId: {}",
                event.getStartupId());

        try {
            // ─────────────────────────────────
            // Cancel all PENDING invitations
            // ─────────────────────────────────
            List<Invitation> pendingInvitations =
                    invitationRepository
                            .findByStartupId(
                                    event.getStartupId())
                            .stream()
                            .filter(invitation ->
                                    invitation.getStatus()
                                    == InvitationStatus.PENDING)
                            .toList();

            pendingInvitations.forEach(invitation -> {
                invitation.setStatus(
                        InvitationStatus.CANCELLED);
                invitationRepository.save(invitation);
                log.info("Invitation id: {} cancelled " +
                        "due to startup deletion",
                        invitation.getId());
            });

            log.info("Cancelled {} pending invitations " +
                    "for startupId: {}",
                    pendingInvitations.size(),
                    event.getStartupId());

            // ─────────────────────────────────
            // Soft delete ACTIVE team members
            // ─────────────────────────────────
            List<TeamMember> activeMembers =
                    teamMemberRepository
                            .findByStartupIdAndIsActiveTrue(
                                    event.getStartupId());

            activeMembers.forEach(member -> {
                member.setIsActive(false);
                member.setLeftAt(LocalDateTime.now());
                teamMemberRepository.save(member);
                log.info("TeamMember id: {} marked " +
                        "inactive due to startup deletion",
                        member.getId());
            });

            log.info("Marked {} team members inactive " +
                    "for startupId: {}",
                    activeMembers.size(),
                    event.getStartupId());

        } catch (Exception e) {
            log.error("Failed to process " +
                    "STARTUP_DELETED event " +
                    "for startupId: {} error: {}",
                    event.getStartupId(),
                    e.getMessage());
        }
    }
}