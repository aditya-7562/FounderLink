package com.founderlink.startup.query;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.exception.InvalidSearchException;
import com.founderlink.startup.exception.StartupNotFoundException;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.repository.StartupRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartupQueryService {

    private final StartupRepository startupRepository;
    private final StartupMapper startupMapper;

    /**
     * QUERY: Get active startup by ID.
     * Also used by FeignClient from other services.
     * Cache key = startupId.
     */
    @Cacheable(value = "startupById", key = "#id")
    public StartupResponseDto getStartupById(Long id) {
        log.info("QUERY - getStartupById: id={} (cache miss, hitting DB)", id);
        return startupRepository.findByIdAndIsDeletedFalse(id)
                .map(startupMapper::toResponseDto)
                .orElseThrow(() -> new StartupNotFoundException("Startup not found with id: " + id));
    }

    /**
     * QUERY: Get all active startups.
     * Single shared cache entry.
     */
    @Cacheable(value = "allStartups", key = "'all'")
    public List<StartupResponseDto> getAllStartups() {
        log.info("QUERY - getAllStartups (cache miss, hitting DB)");
        return getAllStartups(Pageable.unpaged()).getContent();
    }

    public Page<StartupResponseDto> getAllStartups(Pageable pageable) {
        log.info("QUERY - getAllStartups: pageable={}", pageable);
        return startupRepository.findByIsDeletedFalse(pageable)
                .map(startupMapper::toResponseDto);
    }

    /**
     * QUERY: Get active startups by founder.
     * Cache key = founderId.
     */
    @Cacheable(value = "startupsByFounder", key = "#founderId")
    public List<StartupResponseDto> getStartupsByFounderId(Long founderId) {
        log.info("QUERY - getStartupsByFounderId: founderId={} (cache miss, hitting DB)", founderId);
        return getStartupsByFounderId(founderId, Pageable.unpaged()).getContent();
    }

    public Page<StartupResponseDto> getStartupsByFounderId(Long founderId, Pageable pageable) {
        log.info("QUERY - getStartupsByFounderId: founderId={}, pageable={}", founderId, pageable);
        return startupRepository.findByFounderIdAndIsDeletedFalse(founderId, pageable)
                .map(startupMapper::toResponseDto);
    }

    /**
     * QUERY: Search startups by filters.
     * Cache key = combination of all filter params.
     */
    @Cacheable(value = "searchStartups",
               key = "(#industry ?: 'null') + '_' + (#stage ?: 'null') + '_' + (#minFunding ?: 'null') + '_' + (#maxFunding ?: 'null')")
    public List<StartupResponseDto> searchStartups(String industry, StartupStage stage,
                                                    BigDecimal minFunding, BigDecimal maxFunding) {
        return searchStartups(industry, stage, minFunding, maxFunding, Pageable.unpaged()).getContent();
    }

    public Page<StartupResponseDto> searchStartups(String industry, StartupStage stage,
                                                   BigDecimal minFunding, BigDecimal maxFunding,
                                                   Pageable pageable) {
        log.info("QUERY - searchStartups: industry={}, stage={}, min={}, max={}, pageable={}",
                industry, stage, minFunding, maxFunding, pageable);

        if (minFunding != null && maxFunding != null) {
            if (minFunding.compareTo(BigDecimal.ZERO) < 0 || maxFunding.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidSearchException("Funding values cannot be negative");
            }
            if (minFunding.compareTo(maxFunding) > 0) {
                throw new InvalidSearchException("Minimum funding cannot be greater than maximum funding");
            }
        }
        if (minFunding != null && minFunding.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidSearchException("Minimum funding cannot be negative");
        }
        if (maxFunding != null && maxFunding.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidSearchException("Maximum funding cannot be negative");
        }

        return startupRepository.searchActiveStartups(industry, stage, minFunding, maxFunding, pageable)
                .map(startupMapper::toResponseDto);
    }

    /**
     * QUERY: Get public aggregated stats.
     */
    @Cacheable(value = "publicStats", key = "'stats'")
    public java.util.Map<String, Object> getPublicStats() {
        log.info("QUERY - getPublicStats (cache miss, hitting DB)");
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("startups", startupRepository.countActiveStartups());
        stats.put("totalFunding", startupRepository.sumActiveFundingGoal());
        return stats;
    }
}
