package com.founderlink.startup.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.founderlink.startup.command.StartupCommandService;
import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.query.StartupQueryService;

@ExtendWith(MockitoExtension.class)
class StartupServiceImplTest {

    @Mock StartupCommandService commandService;
    @Mock StartupQueryService queryService;

    @InjectMocks
    StartupServiceImpl service;

    @Test
    void createStartup_delegatesToCommand() {
        StartupRequestDto req = new StartupRequestDto();
        StartupResponseDto resp = new StartupResponseDto();
        when(commandService.createStartup(1L, req)).thenReturn(resp);

        assertThat(service.createStartup(1L, req)).isSameAs(resp);
    }

    @Test
    void updateStartup_delegatesToCommand() {
        StartupRequestDto req = new StartupRequestDto();
        StartupResponseDto resp = new StartupResponseDto();
        when(commandService.updateStartup(10L, 1L, req)).thenReturn(resp);

        assertThat(service.updateStartup(10L, 1L, req)).isSameAs(resp);
    }

    @Test
    void deleteStartup_delegatesToCommand() {
        doNothing().when(commandService).deleteStartup(10L, 1L);
        service.deleteStartup(10L, 1L);
        verify(commandService).deleteStartup(10L, 1L);
    }

    @Test
    void getStartupById_delegatesToQuery() {
        StartupResponseDto resp = new StartupResponseDto();
        when(queryService.getStartupById(5L)).thenReturn(resp);

        assertThat(service.getStartupById(5L)).isSameAs(resp);
    }

    @Test
    void getAllStartups_list_delegatesToQuery() {
        List<StartupResponseDto> list = List.of(new StartupResponseDto());
        when(queryService.getAllStartups()).thenReturn(list);

        assertThat(service.getAllStartups()).isSameAs(list);
    }

    @Test
    void getAllStartups_pageable_delegatesToQuery() {
        Page<StartupResponseDto> page = new PageImpl<>(List.of());
        when(queryService.getAllStartups(any(Pageable.class))).thenReturn(page);

        assertThat(service.getAllStartups(Pageable.unpaged())).isSameAs(page);
    }

    @Test
    void getStartupsByFounderId_list_delegatesToQuery() {
        List<StartupResponseDto> list = List.of(new StartupResponseDto());
        when(queryService.getStartupsByFounderId(3L)).thenReturn(list);

        assertThat(service.getStartupsByFounderId(3L)).isSameAs(list);
    }

    @Test
    void getStartupsByFounderId_pageable_delegatesToQuery() {
        Page<StartupResponseDto> page = new PageImpl<>(List.of());
        when(queryService.getStartupsByFounderId(eq(3L), any(Pageable.class))).thenReturn(page);

        assertThat(service.getStartupsByFounderId(3L, Pageable.unpaged())).isSameAs(page);
    }

    @Test
    void searchStartups_list_delegatesToQuery() {
        List<StartupResponseDto> list = List.of();
        when(queryService.searchStartups("EdTech", StartupStage.MVP, null, null)).thenReturn(list);

        assertThat(service.searchStartups("EdTech", StartupStage.MVP, null, null)).isSameAs(list);
    }

    @Test
    void searchStartups_pageable_delegatesToQuery() {
        Page<StartupResponseDto> page = new PageImpl<>(List.of());
        when(queryService.searchStartups(eq("EdTech"), eq(StartupStage.MVP),
                any(), any(), any(Pageable.class))).thenReturn(page);

        assertThat(service.searchStartups("EdTech", StartupStage.MVP, null, null, Pageable.unpaged()))
                .isSameAs(page);
    }

    @Test
    void getPublicStats_delegatesToQuery() {
        Map<String, Object> stats = Map.of("startups", 5L);
        when(queryService.getPublicStats()).thenReturn(stats);

        assertThat(service.getPublicStats()).isSameAs(stats);
    }
}
