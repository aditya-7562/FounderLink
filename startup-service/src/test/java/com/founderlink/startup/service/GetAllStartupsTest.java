package com.founderlink.startup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.query.StartupQueryService;
import com.founderlink.startup.repository.StartupRepository;

@ExtendWith(MockitoExtension.class)
class GetAllStartupsTest {

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private StartupMapper startupMapper;

    @InjectMocks
    private StartupQueryService startupService;

    private Startup startup1;
    private Startup startup2;
    private StartupResponseDto responseDto1;
    private StartupResponseDto responseDto2;

    @BeforeEach
    void setUp() {
        startup1 = new Startup();
        startup1.setId(1L);
        startup1.setName("EduReach");
        startup1.setIndustry("EdTech");
        startup1.setFounderId(5L);
        startup1.setIsDeleted(false);
        startup1.setStage(StartupStage.MVP);
        startup1.setFundingGoal(
                new BigDecimal("5000000.00"));
        startup1.setCreatedAt(LocalDateTime.now());

        startup2 = new Startup();
        startup2.setId(2L);
        startup2.setName("HealthTech");
        startup2.setIndustry("HealthTech");
        startup2.setFounderId(10L);
        startup2.setIsDeleted(false);
        startup2.setStage(StartupStage.IDEA);
        startup2.setFundingGoal(
                new BigDecimal("2000000.00"));
        startup2.setCreatedAt(LocalDateTime.now());

        responseDto1 = new StartupResponseDto();
        responseDto1.setId(1L);
        responseDto1.setName("EduReach");
        responseDto1.setFounderId(5L);

        responseDto2 = new StartupResponseDto();
        responseDto2.setId(2L);
        responseDto2.setName("HealthTech");
        responseDto2.setFounderId(10L);
    }


    // SUCCESS

    @Test
    void getAllStartups_Success() {

        // Arrange
        when(startupRepository
                .findByIsDeletedFalse(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(startup1, startup2)));
        when(startupMapper.toResponseDto(startup1))
                .thenReturn(responseDto1);
        when(startupMapper.toResponseDto(startup2))
                .thenReturn(responseDto2);

        // Act
        List<StartupResponseDto> result =
                startupService.getAllStartups();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName())
                .isEqualTo("EduReach");
        assertThat(result.get(1).getName())
                .isEqualTo("HealthTech");
    }

    // EMPTY LIST

    @Test
    void getAllStartups_EmptyList_ReturnsEmpty() {

        // Arrange
        when(startupRepository
                .findByIsDeletedFalse(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        // Act
        List<StartupResponseDto> result =
                startupService.getAllStartups();

        // Assert
        assertThat(result).isEmpty();
    }

    // DELETED EXCLUDED

    @Test
    void getAllStartups_DeletedExcluded() {

        // Arrange
        // findByIsDeletedFalse returns only
        // active startups automatically
        when(startupRepository
                .findByIsDeletedFalse(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(startup1)));
        when(startupMapper.toResponseDto(startup1))
                .thenReturn(responseDto1);

        // Act
        List<StartupResponseDto> result =
                startupService.getAllStartups();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName())
                .isEqualTo("EduReach");
    }
}