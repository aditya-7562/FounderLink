package com.founderlink.startup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.founderlink.startup.exception.InvalidSearchException;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.query.StartupQueryService;
import com.founderlink.startup.repository.StartupRepository;

@ExtendWith(MockitoExtension.class)
class SearchStartupsTest {

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
        responseDto1.setIndustry("EdTech");
        responseDto1.setStage(StartupStage.MVP);

        responseDto2 = new StartupResponseDto();
        responseDto2.setId(2L);
        responseDto2.setName("HealthTech");
        responseDto2.setIndustry("HealthTech");
        responseDto2.setStage(StartupStage.IDEA);
    }

    // ─────────────────────────────────────────
    // NO FILTERS — RETURNS ALL
    // ─────────────────────────────────────────
    @Test
    void searchStartups_NoFilters_ReturnsAll() {

        // Arrange
        when(startupRepository.searchActiveStartups(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(startup1, startup2)));
        when(startupMapper.toResponseDto(startup1))
                .thenReturn(responseDto1);
        when(startupMapper.toResponseDto(startup2))
                .thenReturn(responseDto2);

        // Act
        List<StartupResponseDto> result =
                startupService.searchStartups(
                        null, null, null, null);

        // Assert
        assertThat(result).hasSize(2);
    }

    // ─────────────────────────────────────────
    // FILTER BY INDUSTRY
    // ─────────────────────────────────────────
    @Test
    void searchStartups_ByIndustry_Success() {

        // Arrange
        when(startupRepository.searchActiveStartups(
                org.mockito.ArgumentMatchers.eq("EdTech"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(startup1)));
        when(startupMapper.toResponseDto(startup1))
                .thenReturn(responseDto1);

        // Act
        List<StartupResponseDto> result =
                startupService.searchStartups(
                        "EdTech", null, null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIndustry())
                .isEqualTo("EdTech");
    }

    // ─────────────────────────────────────────
    // FILTER BY STAGE
    // ─────────────────────────────────────────
    @Test
    void searchStartups_ByStage_Success() {

        // Arrange
        when(startupRepository.searchActiveStartups(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(StartupStage.MVP),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(startup1)));
        when(startupMapper.toResponseDto(startup1))
                .thenReturn(responseDto1);

        // Act
        List<StartupResponseDto> result =
                startupService.searchStartups(
                        null, StartupStage.MVP,
                        null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStage())
                .isEqualTo(StartupStage.MVP);
    }

    // ─────────────────────────────────────────
    // FILTER BY INDUSTRY AND STAGE
    // ─────────────────────────────────────────
    @Test
    void searchStartups_ByIndustryAndStage_Success() {

        // Arrange
        when(startupRepository.searchActiveStartups(
                org.mockito.ArgumentMatchers.eq("EdTech"),
                org.mockito.ArgumentMatchers.eq(StartupStage.MVP),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(startup1)));
        when(startupMapper.toResponseDto(startup1))
                .thenReturn(responseDto1);

        // Act
        List<StartupResponseDto> result =
                startupService.searchStartups(
                        "EdTech", StartupStage.MVP,
                        null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName())
                .isEqualTo("EduReach");
    }

    // ─────────────────────────────────────────
    // FILTER BY FUNDING RANGE
    // ─────────────────────────────────────────
    @Test
    void searchStartups_ByFundingRange_Success() {

        // Arrange
        when(startupRepository.searchActiveStartups(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("1000000.00")),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("6000000.00")),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(startup1)));
        when(startupMapper.toResponseDto(startup1))
                .thenReturn(responseDto1);

        // Act
        List<StartupResponseDto> result =
                startupService.searchStartups(
                        null, null,
                        new BigDecimal("1000000.00"),
                        new BigDecimal("6000000.00"));

        // Assert
        assertThat(result).hasSize(1);
    }

    // ─────────────────────────────────────────
    // MIN GREATER THAN MAX
    // ─────────────────────────────────────────
    @Test
    void searchStartups_MinGreaterThanMax_ThrowsException() {

        // Act & Assert
        assertThatThrownBy(() ->
                startupService.searchStartups(
                        null, null,
                        new BigDecimal("6000000.00"),
                        new BigDecimal("1000000.00")))
                .isInstanceOf(
                        InvalidSearchException.class)
                .hasMessage(
                        "Minimum funding cannot be " +
                        "greater than maximum funding");
    }

    // ─────────────────────────────────────────
    // NEGATIVE FUNDING
    // ─────────────────────────────────────────
    @Test
    void searchStartups_NegativeFunding_ThrowsException() {

        // Act & Assert
        assertThatThrownBy(() ->
                startupService.searchStartups(
                        null, null,
                        new BigDecimal("-1000.00"),
                        new BigDecimal("6000000.00")))
                .isInstanceOf(
                        InvalidSearchException.class)
                .hasMessage(
                        "Funding values cannot be negative");
    }

    // ─────────────────────────────────────────
    // EMPTY RESULTS
    // ─────────────────────────────────────────
    @Test
    void searchStartups_NoResults_ReturnsEmpty() {

        // Arrange
        when(startupRepository.searchActiveStartups(
                org.mockito.ArgumentMatchers.eq("BlockChain"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        // Act
        List<StartupResponseDto> result =
                startupService.searchStartups(
                        "BlockChain",
                        null, null, null);

        // Assert
        assertThat(result).isEmpty();
    }
}