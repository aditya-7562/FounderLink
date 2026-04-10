package com.founderlink.startup.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.StartupStage;

public interface StartupService {

    // ─────────────────────────────────────────
    // CREATE STARTUP
    // Called by → Founder
    // Edge Cases:
    // → validation handled by @Valid
    // → founderId from header
    // → publish STARTUP_CREATED event
    // ─────────────────────────────────────────
    StartupResponseDto createStartup(
            Long founderId,
            StartupRequestDto requestDto);

    // ─────────────────────────────────────────
    // GET STARTUP BY ID
    // Called by → Everyone + FeignClient
    // Edge Cases:
    // → startup not found
    // → startup is deleted
    // ─────────────────────────────────────────
    StartupResponseDto getStartupById(Long id);

    // ─────────────────────────────────────────
    // GET ALL STARTUPS
    // Called by → Investor, Founder, Admin
    // Edge Cases:
    // → returns empty list if none
    // → excludes deleted startups
    // ─────────────────────────────────────────
    List<StartupResponseDto> getAllStartups();

    Page<StartupResponseDto> getAllStartups(Pageable pageable);

    // ─────────────────────────────────────────
    // GET STARTUPS BY FOUNDER
    // Called by → Founder
    // Edge Cases:
    // → founderId from header
    // → returns empty list if none
    // → excludes deleted startups
    // ─────────────────────────────────────────
    List<StartupResponseDto> getStartupsByFounderId(
            Long founderId);

    Page<StartupResponseDto> getStartupsByFounderId(
            Long founderId,
            Pageable pageable);

    // ─────────────────────────────────────────
    // UPDATE STARTUP
    // Called by → Founder
    // Edge Cases:
    // → startup not found
    // → startup is deleted
    // → founder does not own startup
    // → validation handled by @Valid
    // ─────────────────────────────────────────
    StartupResponseDto updateStartup(
            Long id,
            Long founderId,
            StartupRequestDto requestDto);

    // ─────────────────────────────────────────
    // DELETE STARTUP (Soft Delete)
    // Called by → Founder
    // Edge Cases:
    // → startup not found
    // → startup already deleted
    // → founder does not own startup
    // ─────────────────────────────────────────
    void deleteStartup(
            Long id,
            Long founderId);

    // ─────────────────────────────────────────
    // SEARCH STARTUPS
    // Called by → Investor, Founder, Admin
    // Edge Cases:
    // → all params null → return all
    // → no results → return empty list
    // → excludes deleted startups
    // → funding range validation
    // ─────────────────────────────────────────
    List<StartupResponseDto> searchStartups(
            String industry,
            StartupStage stage,
            BigDecimal minFunding,
            BigDecimal maxFunding);

    Page<StartupResponseDto> searchStartups(
            String industry,
            StartupStage stage,
            BigDecimal minFunding,
            BigDecimal maxFunding,
            Pageable pageable);

    // ─────────────────────────────────────────
    // GET PUBLIC STATS
    // Returns active startup count and total funding
    // ─────────────────────────────────────────
    java.util.Map<String, Object> getPublicStats();
}
