package com.founderlink.startup.command;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.events.StartupCreatedEvent;
import com.founderlink.startup.events.StartupDeletedEvent;
import com.founderlink.startup.events.StartupEventPublisher;
import com.founderlink.startup.exception.ForbiddenAccessException;
import com.founderlink.startup.exception.StartupNotFoundException;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.repository.StartupRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StartupCommandService {

    private final StartupRepository startupRepository;
    private final StartupMapper startupMapper;
    private final StartupEventPublisher eventPublisher;

    /**
     * COMMAND: Create a new startup.
     * Evicts allStartups and startupsByFounder caches.
     */
    @Caching(evict = {
        @CacheEvict(value = "allStartups",        allEntries = true),
        @CacheEvict(value = "startupsByFounder",  key = "#founderId"),
        @CacheEvict(value = "searchStartups",     allEntries = true)
    })
    public StartupResponseDto createStartup(Long founderId, StartupRequestDto requestDto) {
        log.info("COMMAND - createStartup: founderId={}", founderId);

        Startup startup = startupMapper.toEntity(requestDto, founderId);
        Startup saved = startupRepository.save(startup);

        eventPublisher.publishStartupCreatedEvent(new StartupCreatedEvent(
                saved.getId(), saved.getName(), saved.getFounderId(),
                saved.getIndustry(), saved.getFundingGoal()));

        return startupMapper.toResponseDto(saved);
    }

    /**
     * COMMAND: Update an existing startup.
     * Evicts startupById, allStartups, startupsByFounder and search caches.
     */
    @Caching(evict = {
        @CacheEvict(value = "startupById",       key = "#id"),
        @CacheEvict(value = "allStartups",       allEntries = true),
        @CacheEvict(value = "startupsByFounder", key = "#founderId"),
        @CacheEvict(value = "searchStartups",    allEntries = true)
    })
    public StartupResponseDto updateStartup(Long id, Long founderId, StartupRequestDto requestDto) {
        log.info("COMMAND - updateStartup: id={}, founderId={}", id, founderId);

        Startup startup = startupRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new StartupNotFoundException("Startup not found with id: " + id));

        if (!startup.getFounderId().equals(founderId)) {
            throw new ForbiddenAccessException("You are not authorized to update this startup");
        }

        startup.setName(requestDto.getName());
        startup.setDescription(requestDto.getDescription());
        startup.setIndustry(requestDto.getIndustry());
        startup.setProblemStatement(requestDto.getProblemStatement());
        startup.setSolution(requestDto.getSolution());
        startup.setFundingGoal(requestDto.getFundingGoal());
        startup.setStage(requestDto.getStage());

        return startupMapper.toResponseDto(startupRepository.save(startup));
    }

    /**
     * COMMAND: Soft delete a startup.
     * Evicts all caches since the startup disappears from all query results.
     */
    @Caching(evict = {
        @CacheEvict(value = "startupById",       key = "#id"),
        @CacheEvict(value = "allStartups",       allEntries = true),
        @CacheEvict(value = "startupsByFounder", key = "#founderId"),
        @CacheEvict(value = "searchStartups",    allEntries = true)
    })
    public void deleteStartup(Long id, Long founderId) {
        log.info("COMMAND - deleteStartup: id={}, founderId={}", id, founderId);

        Startup startup = startupRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new StartupNotFoundException("Startup not found with id: " + id));

        if (!startup.getFounderId().equals(founderId)) {
            throw new ForbiddenAccessException("You are not authorized to delete this startup");
        }

        startup.setIsDeleted(true);
        startupRepository.save(startup);

        eventPublisher.publishStartupDeletedEvent(
                new StartupDeletedEvent(startup.getId(), startup.getFounderId()));
    }
}
