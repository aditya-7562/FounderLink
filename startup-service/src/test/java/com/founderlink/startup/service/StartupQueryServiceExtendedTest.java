package com.founderlink.startup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.query.StartupQueryService;
import com.founderlink.startup.repository.StartupRepository;

@ExtendWith(MockitoExtension.class)
class StartupQueryServiceExtendedTest {

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private StartupMapper startupMapper;

    @InjectMocks
    private StartupQueryService queryService;

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
        startup.setFundingGoal(new BigDecimal("5000000.00"));

        responseDto = new StartupResponseDto();
        responseDto.setId(1L);
        responseDto.setName("EduReach");
        responseDto.setFounderId(5L);
    }

    @Test
    void getAllStartups_pageable_returnsMappedPage() {
        Page<Startup> entityPage = new PageImpl<>(List.of(startup));
        when(startupRepository.findByIsDeletedFalse(any(Pageable.class))).thenReturn(entityPage);
        when(startupMapper.toResponseDto(startup)).thenReturn(responseDto);

        Pageable pageable = PageRequest.of(0, 9);
        Page<StartupResponseDto> result = queryService.getAllStartups(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("EduReach");
    }

    @Test
    void getStartupsByFounderId_pageable_returnsMappedPage() {
        Page<Startup> entityPage = new PageImpl<>(List.of(startup));
        when(startupRepository.findByFounderIdAndIsDeletedFalse(
                org.mockito.ArgumentMatchers.eq(5L), any(Pageable.class))).thenReturn(entityPage);
        when(startupMapper.toResponseDto(startup)).thenReturn(responseDto);

        Pageable pageable = PageRequest.of(0, 9);
        Page<StartupResponseDto> result = queryService.getStartupsByFounderId(5L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void searchStartups_pageable_returnsMappedPage() {
        Page<Startup> entityPage = new PageImpl<>(List.of(startup));
        when(startupRepository.searchActiveStartups(
                org.mockito.ArgumentMatchers.eq("EdTech"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(Pageable.class))).thenReturn(entityPage);
        when(startupMapper.toResponseDto(startup)).thenReturn(responseDto);

        Pageable pageable = PageRequest.of(0, 9);
        Page<StartupResponseDto> result = queryService.searchStartups("EdTech", null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getPublicStats_returnsStartupsAndFunding() {
        when(startupRepository.countActiveStartups()).thenReturn(5L);
        when(startupRepository.sumActiveFundingGoal()).thenReturn(new BigDecimal("10000000"));

        Map<String, Object> stats = queryService.getPublicStats();

        assertThat(stats.get("startups")).isEqualTo(5L);
        assertThat(stats.get("totalFunding")).isEqualTo(new BigDecimal("10000000"));
    }

    @Test
    void searchStartups_onlyMaxFundingNegative_throwsException() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                queryService.searchStartups(null, null, null,
                        new BigDecimal("-500"), Pageable.unpaged()))
                .isInstanceOf(com.founderlink.startup.exception.InvalidSearchException.class)
                .hasMessage("Maximum funding cannot be negative");
    }

    @Test
    void searchStartups_onlyMinFundingNegative_throwsException() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                queryService.searchStartups(null, null,
                        new BigDecimal("-500"), null, Pageable.unpaged()))
                .isInstanceOf(com.founderlink.startup.exception.InvalidSearchException.class)
                .hasMessage("Minimum funding cannot be negative");
    }
}
