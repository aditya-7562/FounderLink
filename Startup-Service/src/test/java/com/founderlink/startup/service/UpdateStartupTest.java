package com.founderlink.startup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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
import com.founderlink.startup.events.StartupEventPublisher;
import com.founderlink.startup.exception.ForbiddenAccessException;
import com.founderlink.startup.exception.StartupNotFoundException;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.repository.StartupRepository;
import com.founderlink.startup.serviceImpl.StartupServiceImpl;

@ExtendWith(MockitoExtension.class)
class UpdateStartupTest {

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private StartupMapper startupMapper;

    @Mock
    private StartupEventPublisher eventPublisher;

    @InjectMocks
    private StartupServiceImpl startupService;

    private Startup startup;
    private StartupRequestDto requestDto;
    private StartupResponseDto responseDto;

    @BeforeEach
    void setUp() {
        startup = new Startup();
        startup.setId(1L);
        startup.setName("EduReach");
        startup.setDescription("Old description");
        startup.setIndustry("EdTech");
        startup.setProblemStatement("Old problem");
        startup.setSolution("Old solution");
        startup.setFundingGoal(
                new BigDecimal("5000000.00"));
        startup.setStage(StartupStage.MVP);
        startup.setFounderId(5L);
        startup.setIsDeleted(false);
        startup.setCreatedAt(LocalDateTime.now());

        requestDto = new StartupRequestDto();
        requestDto.setName("EduReach Updated");
        requestDto.setDescription("New description");
        requestDto.setIndustry("EdTech");
        requestDto.setProblemStatement("New problem");
        requestDto.setSolution("New solution");
        requestDto.setFundingGoal(
                new BigDecimal("8000000.00"));
        requestDto.setStage(StartupStage.EARLY_TRACTION);

        responseDto = new StartupResponseDto();
        responseDto.setId(1L);
        responseDto.setName("EduReach Updated");
        responseDto.setFounderId(5L);
        responseDto.setStage(
                StartupStage.EARLY_TRACTION);
    }

   
    // SUCCESS
    
    @Test
    void updateStartup_Success() {

        // Arrange
        when(startupRepository
                .findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(startup));
        when(startupRepository.save(
                any(Startup.class)))
                .thenReturn(startup);
        when(startupMapper.toResponseDto(startup))
                .thenReturn(responseDto);

        // Act
        StartupResponseDto result = startupService
                .updateStartup(1L, 5L, requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName())
                .isEqualTo("EduReach Updated");
        assertThat(result.getStage())
                .isEqualTo(StartupStage.EARLY_TRACTION);

        verify(startupRepository, times(1))
                .save(any(Startup.class));
    }

    // NOT FOUND

    @Test
    void updateStartup_NotFound_ThrowsException() {

        // Arrange
        when(startupRepository
                .findByIdAndIsDeletedFalse(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                startupService
                        .updateStartup(
                                999L, 5L, requestDto))
                .isInstanceOf(
                        StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 999");

        verify(startupRepository, never())
                .save(any(Startup.class));
    }

 
    // NOT OWNER

    @Test
    void updateStartup_NotOwner_ThrowsException() {

        // Arrange
        // startup founderId = 5
        // but founderId 99 trying to update
        when(startupRepository
                .findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(startup));

        // Act & Assert
        assertThatThrownBy(() ->
                startupService
                        .updateStartup(
                                1L, 99L, requestDto))
                .isInstanceOf(
                        ForbiddenAccessException.class)
                .hasMessage(
                        "You are not authorized " +
                        "to update this startup");

        verify(startupRepository, never())
                .save(any(Startup.class));
    }

    // STAGE UPDATE

    @Test
    void updateStartup_StageChange_Success() {

        // Arrange
        startup.setStage(StartupStage.IDEA);
        requestDto.setStage(StartupStage.SCALING);

        StartupResponseDto scalingResponse =
                new StartupResponseDto();
        scalingResponse.setId(1L);
        scalingResponse.setStage(StartupStage.SCALING);

        when(startupRepository
                .findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(startup));
        when(startupRepository.save(
                any(Startup.class)))
                .thenReturn(startup);
        when(startupMapper.toResponseDto(startup))
                .thenReturn(scalingResponse);

       
        StartupResponseDto result = startupService
                .updateStartup(1L, 5L, requestDto);

        assertThat(result.getStage())
                .isEqualTo(StartupStage.SCALING);
    }
}