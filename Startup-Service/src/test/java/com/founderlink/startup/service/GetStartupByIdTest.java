package com.founderlink.startup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.exception.StartupNotFoundException;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.query.StartupQueryService;
import com.founderlink.startup.repository.StartupRepository;

@ExtendWith(MockitoExtension.class)
class GetStartupByIdTest {

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private StartupMapper startupMapper;

    @InjectMocks
    private StartupQueryService startupService;

    private Startup startup;
    private StartupResponseDto responseDto;

    @BeforeEach
    void setUp() {
        startup = new Startup();
        startup.setId(1L);
        startup.setName("EduReach");
        startup.setIndustry("EdTech");
        startup.setFounderId(5L);
        startup.setIsDeleted(false);
        startup.setStage(StartupStage.MVP);
        startup.setFundingGoal(
                new BigDecimal("5000000.00"));
        startup.setCreatedAt(LocalDateTime.now());

        responseDto = new StartupResponseDto();
        responseDto.setId(1L);
        responseDto.setName("EduReach");
        responseDto.setFounderId(5L);
        responseDto.setStage(StartupStage.MVP);
        responseDto.setFundingGoal(
                new BigDecimal("5000000.00"));
        responseDto.setCreatedAt(LocalDateTime.now());
    }

  
    // SUCCESS

    @Test
    void getStartupById_Success() {

        // Arrange
        when(startupRepository
                .findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(startup));
        when(startupMapper.toResponseDto(startup))
                .thenReturn(responseDto);

        // Act
        StartupResponseDto result = startupService
                .getStartupById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName())
                .isEqualTo("EduReach");
        assertThat(result.getFounderId())
                .isEqualTo(5L);
    }

 
    // NOT FOUND

    @Test
    void getStartupById_NotFound_ThrowsException() {

        // Arrange
        when(startupRepository
                .findByIdAndIsDeletedFalse(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                startupService.getStartupById(999L))
                .isInstanceOf(
                        StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 999");
    }

    // DELETED STARTUP

    @Test
    void getStartupById_DeletedStartup_ThrowsException() {

        // Arrange
        // findByIdAndIsDeletedFalse returns empty
        // because startup is deleted
        when(startupRepository
                .findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                startupService.getStartupById(1L))
                .isInstanceOf(
                        StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 1");
    }
}