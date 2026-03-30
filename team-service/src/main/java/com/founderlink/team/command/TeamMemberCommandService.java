package com.founderlink.team.command;

import java.time.LocalDateTime;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.request.JoinTeamRequestDto;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.exception.AlreadyTeamMemberException;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.InvalidInvitationStatusException;
import com.founderlink.team.exception.InvitationNotFoundException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.StartupServiceUnavailableException;
import com.founderlink.team.exception.TeamMemberNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.repository.TeamMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TeamMemberCommandService {

    private final TeamMemberRepository teamMemberRepository;
    private final InvitationRepository invitationRepository;
    private final TeamMemberMapper teamMemberMapper;
    private final StartupServiceClient startupServiceClient;
    private final com.founderlink.team.events.TeamEventPublisher teamEventPublisher;

    // ── joinTeam — no Feign call, no retry needed ────────────────────────────

    @Caching(evict = {
        @CacheEvict(value = "teamByStartup",       allEntries = true),
        @CacheEvict(value = "memberHistory",        key = "#userId"),
        @CacheEvict(value = "activeMemberRoles",    key = "#userId"),
        @CacheEvict(value = "invitationsByUser",    key = "#userId"),
        @CacheEvict(value = "invitationsByStartup", allEntries = true)
    })
    public TeamMemberResponseDto joinTeam(Long userId, JoinTeamRequestDto requestDto) {
        log.info("COMMAND - joinTeam: userId={}, invitationId={}", userId, requestDto.getInvitationId());

        Invitation invitation = invitationRepository.findById(requestDto.getInvitationId())
                .orElseThrow(() -> new InvitationNotFoundException(
                        "Invitation not found with id: " + requestDto.getInvitationId()));

        if (!invitation.getInvitedUserId().equals(userId)) {
            throw new UnauthorizedAccessException("This invitation does not belong to you");
        }

        if (!invitation.getStatus().equals(InvitationStatus.PENDING)) {
            throw new InvalidInvitationStatusException("Only PENDING invitations can be accepted");
        }

        if (teamMemberRepository.existsByStartupIdAndUserIdAndIsActiveTrue(invitation.getStartupId(), userId)) {
            throw new AlreadyTeamMemberException("You are already a member of this startup");
        }

        if (teamMemberRepository.existsByStartupIdAndRoleAndIsActiveTrue(invitation.getStartupId(), invitation.getRole())) {
            throw new AlreadyTeamMemberException("This role is already filled in the team");
        }

        TeamMember teamMember = new TeamMember();
        teamMember.setStartupId(invitation.getStartupId());
        teamMember.setUserId(userId);
        teamMember.setRole(invitation.getRole());

        TeamMember saved = teamMemberRepository.save(teamMember);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        // Publish team member accepted event
        teamEventPublisher.publishTeamMemberAcceptedEvent(
                new com.founderlink.team.events.TeamMemberAcceptedEvent(
                        invitation.getId(),
                        invitation.getStartupId(),
                        invitation.getFounderId(),
                        userId,
                        invitation.getRole().name()
                ));

        return teamMemberMapper.toResponseDto(saved);
    }

    // ── removeTeamMember — calls Feign, needs retry + CB ────────────────────

    @Retry(name = "startupService")
    @CircuitBreaker(name = "startupService", fallbackMethod = "removeTeamMemberFallback")
    @Caching(evict = {
        @CacheEvict(value = "teamByStartup",     allEntries = true),
        @CacheEvict(value = "memberHistory",     allEntries = true),
        @CacheEvict(value = "activeMemberRoles", allEntries = true)
    })
    public void removeTeamMember(Long teamMemberId, Long founderId) {
        log.info("COMMAND - removeTeamMember: teamMemberId={}, founderId={}", teamMemberId, founderId);

        TeamMember teamMember = teamMemberRepository.findById(teamMemberId)
                .orElseThrow(() -> new TeamMemberNotFoundException(
                        "Team member not found with id: " + teamMemberId));

        verifyFounderOwnsStartup(teamMember.getStartupId(), founderId);

        if (teamMember.getUserId().equals(founderId)) {
            throw new UnauthorizedAccessException("Founder cannot remove themselves from the team");
        }

        teamMember.setIsActive(false);
        teamMember.setLeftAt(LocalDateTime.now());
        teamMemberRepository.save(teamMember);
    }

    public void removeTeamMemberFallback(Long teamMemberId, Long founderId, Throwable throwable) {
        if (throwable instanceof TeamMemberNotFoundException
                || throwable instanceof ForbiddenAccessException
                || throwable instanceof StartupNotFoundException
                || throwable instanceof UnauthorizedAccessException) {
            throw (RuntimeException) throwable;
        }
        log.error("FALLBACK - removeTeamMember: circuit open or retries exhausted. Reason: {}", throwable.getMessage());
        throw new StartupServiceUnavailableException(
                "StartupServiceClient#getStartupById",
                "Startup service is temporarily unavailable");
    }

    // ── Private helper ───────────────────────────────────────────────────────

    private void verifyFounderOwnsStartup(Long startupId, Long founderId) {
        StartupResponseDto startup = startupServiceClient.getStartupById(startupId);
        if (startup == null) {
            throw new StartupNotFoundException("Startup not found with id: " + startupId);
        }
        if (!startup.getFounderId().equals(founderId)) {
            throw new ForbiddenAccessException("You are not authorized to perform this action on this startup");
        }
    }
}
