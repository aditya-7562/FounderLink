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
}
