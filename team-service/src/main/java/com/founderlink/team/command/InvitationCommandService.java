package com.founderlink.team.command;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.events.TeamInviteEvent;
import com.founderlink.team.events.TeamMemberRejectedEvent;
import com.founderlink.team.exception.DuplicateInvitationException;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.InvalidInvitationStatusException;
import com.founderlink.team.exception.InvitationNotFoundException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.StartupServiceUnavailableException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InvitationCommandService {

    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;
    private final TeamEventPublisher eventPublisher;
    private final StartupServiceClient startupServiceClient;

    // ── sendInvitation — calls Feign, needs retry + CB ───────────────────────

    @Retry(name = "startupService")
    @CircuitBreaker(name = "startupService", fallbackMethod = "sendInvitationFallback")
    @Caching(evict = {
        @CacheEvict(value = "invitationsByStartup", key = "#requestDto.startupId"),
        @CacheEvict(value = "invitationsByUser",    key = "#requestDto.invitedUserId")
    })
    public InvitationResponseDto sendInvitation(Long founderId, InvitationRequestDto requestDto) {
        log.info("COMMAND - sendInvitation: founderId={}, startupId={}", founderId, requestDto.getStartupId());

        verifyFounderOwnsStartup(requestDto.getStartupId(), founderId);

        if (founderId.equals(requestDto.getInvitedUserId())) {
            throw new UnauthorizedAccessException("You cannot invite yourself to your startup");
        }

        if (invitationRepository.existsByStartupIdAndInvitedUserIdAndStatus(
                requestDto.getStartupId(), requestDto.getInvitedUserId(), InvitationStatus.PENDING)) {
            throw new DuplicateInvitationException("User already has a pending invitation for this startup");
        }

        if (invitationRepository.existsByStartupIdAndRoleAndStatus(
                requestDto.getStartupId(), requestDto.getRole(), InvitationStatus.PENDING)) {
            throw new DuplicateInvitationException("This role already has a pending invitation");
        }

        Invitation invitation = invitationMapper.toEntity(requestDto, founderId);
        Invitation saved = invitationRepository.save(invitation);

        eventPublisher.publishTeamInviteEvent(new TeamInviteEvent(
                saved.getStartupId(), saved.getInvitedUserId(), saved.getRole().name()));

        return invitationMapper.toResponseDto(saved);
    }

    public InvitationResponseDto sendInvitationFallback(Long founderId, InvitationRequestDto requestDto,
                                                         Throwable throwable) {
        if (throwable instanceof StartupNotFoundException
                || throwable instanceof ForbiddenAccessException
                || throwable instanceof DuplicateInvitationException
                || throwable instanceof UnauthorizedAccessException) {
            throw (RuntimeException) throwable;
        }
        log.error("FALLBACK - sendInvitation: circuit open or retries exhausted. Reason: {}", throwable.getMessage());
        throw new StartupServiceUnavailableException(
                "StartupServiceClient#getStartupById",
                "Startup service is temporarily unavailable");
    }

    // ── cancelInvitation — no Feign call, no retry needed ───────────────────

    @Caching(evict = {
        @CacheEvict(value = "invitationsByStartup", allEntries = true),
        @CacheEvict(value = "invitationsByUser",    allEntries = true)
    })
    public InvitationResponseDto cancelInvitation(Long invitationId, Long founderId) {
        log.info("COMMAND - cancelInvitation: invitationId={}, founderId={}", invitationId, founderId);

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found with id: " + invitationId));

        if (!invitation.getFounderId().equals(founderId)) {
            throw new UnauthorizedAccessException("You are not authorized to cancel this invitation");
        }

        if (!invitation.getStatus().equals(InvitationStatus.PENDING)) {
            throw new InvalidInvitationStatusException("Only PENDING invitations can be cancelled");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        return invitationMapper.toResponseDto(invitationRepository.save(invitation));
    }

    // ── rejectInvitation — no Feign call, no retry needed ───────────────────

    @Caching(evict = {
        @CacheEvict(value = "invitationsByUser",    key = "#userId"),
        @CacheEvict(value = "invitationsByStartup", allEntries = true)
    })
    public InvitationResponseDto rejectInvitation(Long invitationId, Long userId) {
        log.info("COMMAND - rejectInvitation: invitationId={}, userId={}", invitationId, userId);

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invitation not found with id: " + invitationId));

        if (!invitation.getInvitedUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to reject this invitation");
        }

        if (!invitation.getStatus().equals(InvitationStatus.PENDING)) {
            throw new InvalidInvitationStatusException("Only PENDING invitations can be rejected");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        Invitation saved = invitationRepository.save(invitation);

        // Publish team member rejected event
        eventPublisher.publishTeamMemberRejectedEvent(
                new TeamMemberRejectedEvent(
                        saved.getId(),
                        saved.getStartupId(),
                        saved.getFounderId(),
                        userId,
                        saved.getRole().name()
                ));

        return invitationMapper.toResponseDto(saved);
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
