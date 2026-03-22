package com.founderlink.startup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.events.StartupCreatedEvent;
import com.founderlink.startup.events.StartupEventPublisher;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.repository.StartupRepository;
import com.founderlink.startup.serviceImpl.StartupServiceImpl;

@ExtendWith(MockitoExtension.class)
class CreateStartupTest {

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private StartupMapper startupMapper;

    @Mock
    private StartupEventPublisher eventPublisher;

    @InjectMocks
    private StartupServiceImpl startupService;

    private StartupRequestDto requestDto;
    private Startup startup;
    private StartupResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = new StartupRequestDto();
        requestDto.setName("EduReach");
        requestDto.setDescription(
                "Online education for rural India");
        requestDto.setIndustry("EdTech");
        requestDto.setProblemStatement(
                "Rural students lack quality education");
        requestDto.setSolution(
                "Affordable offline-first learning app");
        requestDto.setFundingGoal(
                new BigDecimal("5000000.00"));
        requestDto.setStage(StartupStage.MVP);

        startup = new Startup();
        startup.setId(1L);
        startup.setName("EduReach");
        startup.setDescription(
                "Online education for rural India");
        startup.setIndustry("EdTech");
        startup.setProblemStatement(
                "Rural students lack quality education");
        startup.setSolution(
                "Affordable offline-first learning app");
        startup.setFundingGoal(
                new BigDecimal("5000000.00"));
        startup.setStage(StartupStage.MVP);
        startup.setFounderId(5L);
        startup.setIsDeleted(false);
        startup.setCreatedAt(LocalDateTime.now());

        responseDto = new StartupResponseDto();
        responseDto.setId(1L);
        responseDto.setName("EduReach");
        responseDto.setDescription(
                "Online education for rural India");
        responseDto.setIndustry("EdTech");
        responseDto.setProblemStatement(
                "Rural students lack quality education");
        responseDto.setSolution(
                "Affordable offline-first learning app");
        responseDto.setFundingGoal(
                new BigDecimal("5000000.00"));
        responseDto.setStage(StartupStage.MVP);
        responseDto.setFounderId(5L);
        responseDto.setCreatedAt(LocalDateTime.now());
    }

    // SUCCESS
    
    @Test
    void createStartup_Success() {

        // Arrange
        when(startupMapper.toEntity(requestDto, 5L))
                .thenReturn(startup);
        when(startupRepository.save(
                any(Startup.class)))
                .thenReturn(startup);
        when(startupMapper.toResponseDto(startup))
                .thenReturn(responseDto);

        // Act
        StartupResponseDto result = startupService
                .createStartup(5L, requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName())
                .isEqualTo("EduReach");
        assertThat(result.getFounderId())
                .isEqualTo(5L);
        assertThat(result.getStage())
                .isEqualTo(StartupStage.MVP);
        assertThat(result.getFundingGoal())
                .isEqualByComparingTo("5000000.00");

        // Verify
        verify(startupRepository, times(1))
                .save(any(Startup.class));
        verify(eventPublisher, times(1))
                .publishStartupCreatedEvent(
                        any(StartupCreatedEvent.class));
    }

    // VERIFY EVENT PUBLISHED

    @Test
    void createStartup_PublishesEvent() {

        // Arrange
        when(startupMapper.toEntity(requestDto, 5L))
                .thenReturn(startup);
        when(startupRepository.save(
                any(Startup.class)))
                .thenReturn(startup);
        when(startupMapper.toResponseDto(startup))
                .thenReturn(responseDto);

        // Act
        startupService.createStartup(5L, requestDto);

        // Assert event published
        verify(eventPublisher, times(1))
                .publishStartupCreatedEvent(
                        any(StartupCreatedEvent.class));
    }
}