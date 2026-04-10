package com.founderlink.team.query;

import java.util.List;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.StartupServiceUnavailableException;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.repository.TeamMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamMemberQueryService {

    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberMapper teamMemberMapper;
    private final StartupServiceClient startupServiceClient;

    // ── getTeamByStartupId — calls Feign for FOUNDER role, needs retry + CB ──

    @Retry(name = "startupService")
    @CircuitBreaker(name = "startupService", fallbackMethod = "getTeamByStartupIdFallback")
    @Cacheable(value = "teamByStartup", key = "#startupId")
    public List<TeamMemberResponseDto> getTeamByStartupId(Long startupId, Long founderId, String userRole) {
        log.info("QUERY - getTeamByStartupId: startupId={} (cache miss, hitting DB)", startupId);
        if (userRole.equals("ROLE_FOUNDER")) {
            verifyFounderOwnsStartup(startupId, founderId);
        }
        return getTeamByStartupId(startupId, founderId, userRole, Pageable.unpaged()).getContent();
    }

    public Page<TeamMemberResponseDto> getTeamByStartupId(Long startupId, Long founderId, String userRole, Pageable pageable) {
        log.info("QUERY - getTeamByStartupId: startupId={}, pageable={}", startupId, pageable);
        if (userRole.equals("ROLE_FOUNDER")) {
            verifyFounderOwnsStartup(startupId, founderId);
        }
        return teamMemberRepository.findByStartupIdAndIsActiveTrue(startupId, pageable)
                .map(teamMemberMapper::toResponseDto);
    }

    public List<TeamMemberResponseDto> getTeamByStartupIdFallback(Long startupId, Long founderId,
                                                                    String userRole, Throwable throwable) {
        if (throwable instanceof StartupNotFoundException
                || throwable instanceof ForbiddenAccessException) {
            throw (RuntimeException) throwable;
        }
        log.error("FALLBACK - getTeamByStartupId: circuit open or retries exhausted. Reason: {}", throwable.getMessage());
        throw new StartupServiceUnavailableException(
                "StartupServiceClient#getStartupById",
                "Startup service is temporarily unavailable");
    }

    // ── getMemberHistory — no Feign call, no retry needed ───────────────────

    @Cacheable(value = "memberHistory", key = "#userId")
    public List<TeamMemberResponseDto> getMemberHistory(Long userId) {
        log.info("QUERY - getMemberHistory: userId={} (cache miss, hitting DB)", userId);
        return getMemberHistory(userId, Pageable.unpaged()).getContent();
    }

    public Page<TeamMemberResponseDto> getMemberHistory(Long userId, Pageable pageable) {
        log.info("QUERY - getMemberHistory: userId={}, pageable={}", userId, pageable);
        return teamMemberRepository.findByUserId(userId, pageable)
                .map(teamMemberMapper::toResponseDto);
    }

    // ── getActiveMemberRoles — no Feign call, no retry needed ───────────────

    @Cacheable(value = "activeMemberRoles", key = "#userId")
    public List<TeamMemberResponseDto> getActiveMemberRoles(Long userId) {
        log.info("QUERY - getActiveMemberRoles: userId={} (cache miss, hitting DB)", userId);
        return getActiveMemberRoles(userId, Pageable.unpaged()).getContent();
    }

    public Page<TeamMemberResponseDto> getActiveMemberRoles(Long userId, Pageable pageable) {
        log.info("QUERY - getActiveMemberRoles: userId={}, pageable={}", userId, pageable);
        return teamMemberRepository.findByUserIdAndIsActiveTrue(userId, pageable)
                .map(teamMemberMapper::toResponseDto);
    }

    // ── isTeamMember — no Feign call, real-time access control ──────────────

    public boolean isTeamMember(Long startupId, Long userId) {
        return teamMemberRepository.existsByStartupIdAndUserIdAndIsActiveTrue(startupId, userId);
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
