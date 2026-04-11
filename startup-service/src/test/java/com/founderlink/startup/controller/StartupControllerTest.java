package com.founderlink.startup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.service.StartupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = StartupController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ExtendWith(MockitoExtension.class)
class StartupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StartupService startupService;

    @Autowired
    private ObjectMapper objectMapper;

    private StartupResponseDto responseDto;
    private StartupRequestDto requestDto;

    @BeforeEach
    void setUp() {
        responseDto = new StartupResponseDto();
        responseDto.setId(1L);
        responseDto.setName("EduReach");
        responseDto.setFounderId(5L);
        responseDto.setStage(StartupStage.MVP);
        responseDto.setFundingGoal(new BigDecimal("5000000.00"));
        responseDto.setCreatedAt(LocalDateTime.now());

        requestDto = new StartupRequestDto();
        requestDto.setName("EduReach");
        requestDto.setDescription("Online education for rural India");
        requestDto.setIndustry("EdTech");
        requestDto.setProblemStatement("Rural students lack quality education");
        requestDto.setSolution("Affordable offline-first learning app");
        requestDto.setFundingGoal(new BigDecimal("5000000.00"));
        requestDto.setStage(StartupStage.MVP);
    }

    @Test
    void createStartup_Success() throws Exception {
        when(startupService.createStartup(eq(5L), any(StartupRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/startup")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Startup created successfully"))
                .andExpect(jsonPath("$.data.name").value("EduReach"));
    }

    @Test
    void createStartup_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(post("/startup")
                .header("X-User-Id", 202L)
                .header("X-User-Role", "ROLE_INVESTOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getAllStartups_Success() throws Exception {
        when(startupService.getAllStartups()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup")
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startups fetched successfully"))
                .andExpect(jsonPath("$.data[0].name").value("EduReach"));
    }

    @Test
    void getAllStartups_AllowsAnonymousAccess() throws Exception {
        when(startupService.getAllStartups()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startups fetched successfully"))
                .andExpect(jsonPath("$.data[0].name").value("EduReach"));
    }

    @Test
    void getAllStartups_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(get("/startup")
                .header("X-User-Role", "ROLE_UNKNOWN"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getStartupById_Success() throws Exception {
        when(startupService.getStartupById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/startup/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("EduReach"));
    }

    @Test
    void getStartupDetails_Success() throws Exception {
        when(startupService.getStartupById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/startup/details/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startup fetched successfully"))
                .andExpect(jsonPath("$.data.name").value("EduReach"));
    }

    @Test
    void getStartupsByFounder_Success() throws Exception {
        when(startupService.getStartupsByFounderId(5L)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup/founder")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startups fetched successfully"))
                .andExpect(jsonPath("$.data[0].founderId").value(5L));
    }

    @Test
    void updateStartup_Success() throws Exception {
        when(startupService.updateStartup(eq(1L), eq(5L), any(StartupRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/startup/1")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startup updated successfully"));
    }

    @Test
    void deleteStartup_Success() throws Exception {
        doNothing().when(startupService).deleteStartup(1L, 5L);

        mockMvc.perform(delete("/startup/1")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startup deleted successfully"));
    }

    @Test
    void deleteStartup_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(delete("/startup/1")
                .header("X-User-Id", 202L)
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void searchStartups_Success() throws Exception {
        when(startupService.searchStartups("EdTech", StartupStage.MVP, null, null))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup/search")
                .header("X-User-Role", "ROLE_INVESTOR")
                .param("industry", "EdTech")
                .param("stage", "MVP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startups fetched successfully"));
    }

    @Test
    void searchStartups_AllowsAnonymousAccess() throws Exception {
        when(startupService.searchStartups("EdTech", StartupStage.MVP, null, null))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup/search")
                .param("industry", "EdTech")
                .param("stage", "MVP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startups fetched successfully"));
    }

    @Test
    void getAllStartups_Paginated_ReturnsPaginatedResponse() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getAllStartups(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup")
                .param("page", "0")
                .param("size", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("EduReach"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getStartupsByFounder_Paginated_ReturnsPaginatedResponse() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getStartupsByFounderId(eq(5L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup/founder")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .param("page", "0")
                .param("size", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void searchStartups_Paginated_ReturnsPaginatedResponse() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.searchStartups(eq("EdTech"), eq(StartupStage.MVP), any(), any(),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/startup/search")
                .param("industry", "EdTech")
                .param("stage", "MVP")
                .param("page", "0")
                .param("size", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getPublicStats_ReturnsStats() throws Exception {
        when(startupService.getPublicStats())
                .thenReturn(java.util.Map.of("startups", 5L, "totalFunding", new java.math.BigDecimal("100000")));

        mockMvc.perform(get("/startup/public/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startups").value(5));
    }

    @Test
    void getAllStartups_WithSortParam_ReturnsSortedResults() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getAllStartups(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup")
                .param("page", "0")
                .param("sort", "name,asc"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllStartups_WithDescSortParam_ReturnsSortedResults() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getAllStartups(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup")
                .param("page", "0")
                .param("sort", "name,desc"))
                .andExpect(status().isOk());
    }

    // ── resolveSort: single token (no comma → ASC direction) ──────────────
    @Test
    void getAllStartups_SortSingleToken_DefaultsToAsc() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getAllStartups(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup")
                .param("page", "0")
                .param("sort", "name"))        // only one token → ASC branch
                .andExpect(status().isOk());
    }

    // ── resolveSort: blank first token → uses default property ───────────
    @Test
    void getAllStartups_SortBlankFirstToken_UsesDefaultProperty() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getAllStartups(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup")
                .param("page", "0")
                .param("sort", ",desc"))       // blank token[0] → defaultProperty, DESC
                .andExpect(status().isOk());
    }

    // ── resolveSort: blank sort string → uses default sort ───────────────
    @Test
    void getAllStartups_BlankSortString_UsesDefault() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getAllStartups(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup")
                .param("page", "0")
                .param("sort", ""))            // blank → null/blank branch → default sort
                .andExpect(status().isOk());
    }

    // ── buildPageable: oversized size is clamped to 50 ───────────────────
    @Test
    void getAllStartups_OversizedSize_IsClampedTo50() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getAllStartups(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup")
                .param("page", "0")
                .param("size", "999"))         // > 50 → clamped to 50
                .andExpect(status().isOk());
    }

    // ── buildPageable: size=0 clamps to 1 ────────────────────────────────
    @Test
    void getAllStartups_ZeroSize_IsClampedToOne() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.getAllStartups(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/startup")
                .param("page", "0")
                .param("size", "0"))           // < 1 → clamped to 1
                .andExpect(status().isOk());
    }

    // ── Role: COFOUNDER allowed in getAllStartups ─────────────────────────
    @Test
    void getAllStartups_AsCoFounder_ReturnsOk() throws Exception {
        when(startupService.getAllStartups()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup")
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk());
    }

    // ── Role: ADMIN allowed in getAllStartups ─────────────────────────────
    @Test
    void getAllStartups_AsAdmin_ReturnsOk() throws Exception {
        when(startupService.getAllStartups()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup")
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    // ── Role: COFOUNDER allowed in searchStartups ─────────────────────────
    @Test
    void searchStartups_AsCoFounder_ReturnsOk() throws Exception {
        when(startupService.searchStartups(null, null, null, null))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup/search")
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk());
    }

    // ── Role: ADMIN allowed in searchStartups ─────────────────────────────
    @Test
    void searchStartups_AsAdmin_ReturnsOk() throws Exception {
        when(startupService.searchStartups(null, null, null, null))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup/search")
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    // ── Role: FOUNDER allowed in searchStartups ───────────────────────────
    @Test
    void searchStartups_AsFounder_ReturnsOk() throws Exception {
        when(startupService.searchStartups(null, null, null, null))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/startup/search")
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk());
    }

    // ── getStartupsByFounder: wrong role → forbidden ──────────────────────
    @Test
    void getStartupsByFounder_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(get("/startup/founder")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().is4xxClientError());
    }

    // ── searchStartups: bad role → forbidden ─────────────────────────────
    @Test
    void searchStartups_BadRole_Forbidden() throws Exception {
        mockMvc.perform(get("/startup/search")
                .header("X-User-Role", "ROLE_HACKER"))
                .andExpect(status().is4xxClientError());
    }

    // ── updateStartup: wrong role → forbidden ────────────────────────────
    @Test
    void updateStartup_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(put("/startup/1")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_INVESTOR")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestDto)))
                .andExpect(status().is4xxClientError());
    }

    // ── searchStartups paginated: no user role header (anonymous) ─────────
    @Test
    void searchStartups_Paginated_Anonymous_ReturnsOk() throws Exception {
        org.springframework.data.domain.Page<StartupResponseDto> page =
                new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(startupService.searchStartups(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/startup/search")
                .param("page", "0"))           // paginated but no role → allowed
                .andExpect(status().isOk());
    }
}
