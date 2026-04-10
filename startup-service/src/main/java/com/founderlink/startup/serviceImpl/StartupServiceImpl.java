package com.founderlink.startup.serviceImpl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.founderlink.startup.command.StartupCommandService;
import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.query.StartupQueryService;
import com.founderlink.startup.service.StartupService;

import lombok.RequiredArgsConstructor;

/**
 * Facade that satisfies the existing StartupService contract.
 * Delegates writes → StartupCommandService (CQRS Command side)
 * Delegates reads  → StartupQueryService   (CQRS Query side + Redis cache)
 */
@Service
@RequiredArgsConstructor
public class StartupServiceImpl implements StartupService {

    private final StartupCommandService commandService;
    private final StartupQueryService   queryService;

    @Override
    public StartupResponseDto createStartup(Long founderId, StartupRequestDto requestDto) {
        return commandService.createStartup(founderId, requestDto);
    }

    @Override
    public StartupResponseDto updateStartup(Long id, Long founderId, StartupRequestDto requestDto) {
        return commandService.updateStartup(id, founderId, requestDto);
    }

    @Override
    public void deleteStartup(Long id, Long founderId) {
        commandService.deleteStartup(id, founderId);
    }

    @Override
    public StartupResponseDto getStartupById(Long id) {
        return queryService.getStartupById(id);
    }

    @Override
    public List<StartupResponseDto> getAllStartups() {
        return queryService.getAllStartups();
    }

    @Override
    public Page<StartupResponseDto> getAllStartups(Pageable pageable) {
        return queryService.getAllStartups(pageable);
    }

    @Override
    public List<StartupResponseDto> getStartupsByFounderId(Long founderId) {
        return queryService.getStartupsByFounderId(founderId);
    }

    @Override
    public Page<StartupResponseDto> getStartupsByFounderId(Long founderId, Pageable pageable) {
        return queryService.getStartupsByFounderId(founderId, pageable);
    }

    @Override
    public List<StartupResponseDto> searchStartups(String industry, StartupStage stage,
                                                    BigDecimal minFunding, BigDecimal maxFunding) {
        return queryService.searchStartups(industry, stage, minFunding, maxFunding);
    }

    @Override
    public Page<StartupResponseDto> searchStartups(String industry, StartupStage stage,
                                                   BigDecimal minFunding, BigDecimal maxFunding,
                                                   Pageable pageable) {
        return queryService.searchStartups(industry, stage, minFunding, maxFunding, pageable);
    }

    @Override
    public java.util.Map<String, Object> getPublicStats() {
        return queryService.getPublicStats();
    }
}
