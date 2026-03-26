package com.founderlink.startup.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
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
        return startupRepository.findByIsDeletedFalse()
                .stream()
                .map(startupMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * QUERY: Get active startups by founder.
     * Cache key = founderId.
     */
    @Cacheable(value = "startupsByFounder", key = "#founderId")
    public List<StartupResponseDto> getStartupsByFounderId(Long founderId) {
        log.info("QUERY - getStartupsByFounderId: founderId={} (cache miss, hitting DB)", founderId);
        return startupRepository.findByFounderIdAndIsDeletedFalse(founderId)
                .stream()
                .map(startupMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * QUERY: Search startups by filters.
     * Cache key = combination of all filter params.
     */
    @Cacheable(value = "searchStartups",
               key = "(#industry ?: 'null') + '_' + (#stage ?: 'null') + '_' + (#minFunding ?: 'null') + '_' + (#maxFunding ?: 'null')")
    public List<StartupResponseDto> searchStartups(String industry, StartupStage stage,
                                                    BigDecimal minFunding, BigDecimal maxFunding) {
        log.info("QUERY - searchStartups: industry={}, stage={}, min={}, max={} (cache miss, hitting DB)",
                industry, stage, minFunding, maxFunding);

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

        List<Startup> startups;

        if (industry != null && stage != null && minFunding != null && maxFunding != null) {
            startups = startupRepository.findByIndustryAndStageAndIsDeletedFalse(industry, stage)
                    .stream()
                    .filter(s -> s.getFundingGoal().compareTo(minFunding) >= 0
                              && s.getFundingGoal().compareTo(maxFunding) <= 0)
                    .collect(Collectors.toList());
        } else if (industry != null && stage != null) {
            startups = startupRepository.findByIndustryAndStageAndIsDeletedFalse(industry, stage);
        } else if (industry != null && minFunding != null && maxFunding != null) {
            startups = startupRepository.findByIndustryAndFundingGoalBetweenAndIsDeletedFalse(industry, minFunding, maxFunding);
        } else if (minFunding != null && maxFunding != null) {
            startups = startupRepository.findByFundingGoalBetweenAndIsDeletedFalse(minFunding, maxFunding);
        } else if (industry != null) {
            startups = startupRepository.findByIndustryAndIsDeletedFalse(industry);
        } else if (stage != null) {
            startups = startupRepository.findByStageAndIsDeletedFalse(stage);
        } else {
            startups = startupRepository.findByIsDeletedFalse();
        }

        return startups.stream()
                .map(startupMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
