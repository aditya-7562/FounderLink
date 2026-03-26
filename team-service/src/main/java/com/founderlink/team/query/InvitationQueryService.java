package com.founderlink.team.query;

import java.util.List;
import java.util.stream.Collectors;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
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
        return invitationRepository.findByInvitedUserId(userId)
                .stream()
                .map(invitationMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ── getInvitationsByStartupId — calls Feign, needs retry + CB ───────────

    @Retry(name = "startupService")
    @CircuitBreaker(name = "startupService", fallbackMethod = "getInvitationsByStartupIdFallback")
    @Cacheable(value = "invitationsByStartup", key = "#startupId")
    public List<InvitationResponseDto> getInvitationsByStartupId(Long startupId, Long founderId) {
        log.info("QUERY - getInvitationsByStartupId: startupId={} (cache miss, hitting DB)", startupId);
        verifyFounderOwnsStartup(startupId, founderId);
        return invitationRepository.findByStartupId(startupId)
                .stream()
                .map(invitationMapper::toResponseDto)
                .collect(Collectors.toList());
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
