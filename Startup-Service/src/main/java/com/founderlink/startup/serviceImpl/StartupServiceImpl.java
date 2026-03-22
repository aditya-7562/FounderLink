package com.founderlink.startup.serviceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.events.StartupCreatedEvent;
import com.founderlink.startup.events.StartupDeletedEvent;
import com.founderlink.startup.events.StartupEventPublisher;
import com.founderlink.startup.exception.ForbiddenAccessException;
import com.founderlink.startup.exception.InvalidSearchException;
import com.founderlink.startup.exception.StartupNotFoundException;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.repository.StartupRepository;
import com.founderlink.startup.service.StartupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StartupServiceImpl implements StartupService {

    private final StartupRepository startupRepository;
    private final StartupMapper startupMapper;
    private final StartupEventPublisher eventPublisher;

    // ─────────────────────────────────────────
    // CREATE STARTUP
    // ─────────────────────────────────────────
    @Override
    public StartupResponseDto createStartup(
            Long founderId,
            StartupRequestDto requestDto) {

        // Map DTO to Entity
        Startup startup = startupMapper
                .toEntity(requestDto, founderId);

        // Save to DB
        Startup savedStartup = startupRepository
                .save(startup);

        // Publish RabbitMQ Event
        StartupCreatedEvent event = new StartupCreatedEvent(
                savedStartup.getId(),
                savedStartup.getName(),
                savedStartup.getFounderId(),
                savedStartup.getIndustry(),
                savedStartup.getFundingGoal()
        );
        eventPublisher.publishStartupCreatedEvent(event);

        log.info("Startup created with id: {} " +
                "by founderId: {}",
                savedStartup.getId(), founderId);

        return startupMapper.toResponseDto(savedStartup);
    }

    // ─────────────────────────────────────────
    // GET STARTUP BY ID
    // ─────────────────────────────────────────
    @Override
    public StartupResponseDto getStartupById(Long id) {

        // Find active startup only
        Startup startup = startupRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new StartupNotFoundException(
                        "Startup not found with id: " + id));

        return startupMapper.toResponseDto(startup);
    }

    // ─────────────────────────────────────────
    // GET ALL STARTUPS
    // ─────────────────────────────────────────
    @Override
    public List<StartupResponseDto> getAllStartups() {

        // Only active startups
        return startupRepository
                .findByIsDeletedFalse()
                .stream()
                .map(startupMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────
    // GET STARTUPS BY FOUNDER ID
    // ─────────────────────────────────────────
    @Override
    public List<StartupResponseDto> getStartupsByFounderId(
            Long founderId) {

        // Only active startups by founder
        return startupRepository
                .findByFounderIdAndIsDeletedFalse(founderId)
                .stream()
                .map(startupMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────
    // UPDATE STARTUP
    // ─────────────────────────────────────────
    @Override
    public StartupResponseDto updateStartup(
            Long id,
            Long founderId,
            StartupRequestDto requestDto) {

        // Find active startup only
        Startup startup = startupRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new StartupNotFoundException(
                        "Startup not found with id: " + id));

        // Verify founder owns startup
        if (!startup.getFounderId().equals(founderId)) {
            throw new ForbiddenAccessException(
                    "You are not authorized " +
                    "to update this startup");
        }

        // Update fields
        startup.setName(requestDto.getName());
        startup.setDescription(requestDto.getDescription());
        startup.setIndustry(requestDto.getIndustry());
        startup.setProblemStatement(
                requestDto.getProblemStatement());
        startup.setSolution(requestDto.getSolution());
        startup.setFundingGoal(requestDto.getFundingGoal());
        startup.setStage(requestDto.getStage());

        // Save updated startup
        Startup updatedStartup = startupRepository
                .save(startup);

        log.info("Startup updated with id: {} " +
                "by founderId: {}",
                id, founderId);

        return startupMapper.toResponseDto(updatedStartup);
    }

    // DELETE STARTUP — Soft Delete
    
    @Override
    public void deleteStartup(
            Long id,
            Long founderId) {

        // Find active startup only
        Startup startup = startupRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new StartupNotFoundException(
                                "Startup not found with id: "
                                + id));

        // Verify founder owns startup
        if (!startup.getFounderId().equals(founderId)) {
            throw new ForbiddenAccessException(
                    "You are not authorized " +
                    "to delete this startup");
        }

        // Soft delete
        startup.setIsDeleted(true);
        startupRepository.save(startup);

        // Publish STARTUP_DELETED event    ← ADD
        StartupDeletedEvent event =
                new StartupDeletedEvent(
                        startup.getId(),
                        startup.getFounderId());
        eventPublisher.publishStartupDeletedEvent(event);

        log.info("Startup soft deleted with id: {} " +
                "by founderId: {}",
                id, founderId);
    }

    // SEARCH STARTUPS
    
    @Override
    public List<StartupResponseDto> searchStartups(
            String industry,
            StartupStage stage,
            BigDecimal minFunding,
            BigDecimal maxFunding) {

        // Edge case — validate funding range
        if (minFunding != null && maxFunding != null) {

            // Negative values
            if (minFunding.compareTo(BigDecimal.ZERO) < 0 ||
                maxFunding.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidSearchException(
                        "Funding values cannot be negative");
            }

            // Min greater than max
            if (minFunding.compareTo(maxFunding) > 0) {
                throw new InvalidSearchException(
                        "Minimum funding cannot be greater " +
                        "than maximum funding");
            }
        }

        // Single negative value
        if (minFunding != null &&
                minFunding.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidSearchException(
                    "Minimum funding cannot be negative");
        }

        if (maxFunding != null &&
                maxFunding.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidSearchException(
                    "Maximum funding cannot be negative");
        }

        List<Startup> startups;

        // All combinations of filters
        if (industry != null && stage != null
                && minFunding != null && maxFunding != null) {
            // industry + stage + funding range
            startups = startupRepository
                    .findByIndustryAndStageAndIsDeletedFalse(
                            industry, stage)
                    .stream()
                    .filter(s -> s.getFundingGoal()
                            .compareTo(minFunding) >= 0 &&
                            s.getFundingGoal()
                            .compareTo(maxFunding) <= 0)
                    .collect(Collectors.toList());

        } else if (industry != null && stage != null) {
            // industry + stage
            startups = startupRepository
                    .findByIndustryAndStageAndIsDeletedFalse(
                            industry, stage);

        } else if (industry != null
                && minFunding != null
                && maxFunding != null) {
            // industry + funding range
            startups = startupRepository
                    .findByIndustryAndFundingGoalBetweenAndIsDeletedFalse(
                            industry, minFunding, maxFunding);

        } else if (minFunding != null
                && maxFunding != null) {
            // funding range only
            startups = startupRepository
                    .findByFundingGoalBetweenAndIsDeletedFalse(
                            minFunding, maxFunding);

        } else if (industry != null) {
            // industry only
            startups = startupRepository
                    .findByIndustryAndIsDeletedFalse(industry);

        } else if (stage != null) {
            // stage only
            startups = startupRepository
                    .findByStageAndIsDeletedFalse(stage);

        } else {
            // no filters → return all active
            startups = startupRepository
                    .findByIsDeletedFalse();
        }

        return startups.stream()
                .map(startupMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
