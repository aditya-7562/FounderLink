package com.founderlink.team.query;

import java.util.List;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.StartupServiceUnavailableException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationQueryService {

    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;
    private final StartupServiceClient startupServiceClient;

    // ── getInvitationsByUserId — no Feign call, no retry needed ─────────────

    @Cacheable(value = "invitationsByUser", key = "#userId")
    public List<InvitationResponseDto> getInvitationsByUserId(Long userId) {
        log.info("QUERY - getInvitationsByUserId: userId={} (cache miss, hitting DB)", userId);
        return getInvitationsByUserId(userId, Pageable.unpaged()).getContent();
    }

    public Page<InvitationResponseDto> getInvitationsByUserId(Long userId, Pageable pageable) {
        log.info("QUERY - getInvitationsByUserId: userId={}, pageable={}", userId, pageable);
        return invitationRepository.findByInvitedUserId(userId, pageable)
                .map(invitationMapper::toResponseDto);
    }

    // ── getInvitationsByStartupId — calls Feign, needs retry + CB ───────────

    @Retry(name = "startupService")
    @CircuitBreaker(name = "startupService", fallbackMethod = "getInvitationsByStartupIdFallback")
    @Cacheable(value = "invitationsByStartup", key = "#startupId")
    public List<InvitationResponseDto> getInvitationsByStartupId(Long startupId, Long founderId) {
        log.info("QUERY - getInvitationsByStartupId: startupId={} (cache miss, hitting DB)", startupId);
        verifyFounderOwnsStartup(startupId, founderId);
        return getInvitationsByStartupId(startupId, founderId, Pageable.unpaged()).getContent();
    }

    public Page<InvitationResponseDto> getInvitationsByStartupId(Long startupId, Long founderId, Pageable pageable) {
        log.info("QUERY - getInvitationsByStartupId: startupId={}, founderId={}, pageable={}", startupId, founderId, pageable);
        verifyFounderOwnsStartup(startupId, founderId);
        return invitationRepository.findByStartupId(startupId, pageable)
                .map(invitationMapper::toResponseDto);
    }

    public List<InvitationResponseDto> getInvitationsByStartupIdFallback(Long startupId, Long founderId,
                                                                           Throwable throwable) {
        if (throwable instanceof StartupNotFoundException
                || throwable instanceof ForbiddenAccessException) {
            throw (RuntimeException) throwable;
        }
        log.error("FALLBACK - getInvitationsByStartupId: circuit open or retries exhausted. Reason: {}", throwable.getMessage());
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
