package com.founderlink.investment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.entity.ManualInvestmentStatus;
import com.founderlink.investment.service.InvestmentService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.TestPropertySource;

@WebMvcTest(value = InvestmentController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false"
})
@ExtendWith(MockitoExtension.class)
class InvestmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvestmentService investmentService;

    @Autowired
    private ObjectMapper objectMapper;

    private InvestmentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        responseDto = new InvestmentResponseDto();
        responseDto.setId(1L);
        responseDto.setStartupId(101L);
        responseDto.setInvestorId(202L);
        responseDto.setAmount(new BigDecimal("1000000.00"));
        responseDto.setStatus(InvestmentStatus.PENDING);
        responseDto.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createInvestment_Success() throws Exception {
        InvestmentRequestDto request = new InvestmentRequestDto();
        request.setStartupId(101L);
        request.setAmount(new BigDecimal("1000000.00"));

        when(investmentService.createInvestment(eq(202L), any(InvestmentRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/investments")
                .header("X-User-Id", 202L)
                .header("X-User-Role", "ROLE_INVESTOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Investment created successfully"))
                .andExpect(jsonPath("$.data.startupId").value(101L));
    }

    @Test
    void createInvestment_WrongRole_Forbidden() throws Exception {
        InvestmentRequestDto request = new InvestmentRequestDto();
        request.setStartupId(101L);
        request.setAmount(new BigDecimal("1000000.00"));

        mockMvc.perform(post("/investments")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getInvestmentsByStartupId_Success() throws Exception {
        when(investmentService.getInvestmentsByStartupId(101L, 5L))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/investments/startup/101")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Investments fetched successfully"))
                .andExpect(jsonPath("$.data[0].startupId").value(101L));
    }

    @Test
    void getInvestmentsByStartupId_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(get("/investments/startup/101")
                .header("X-User-Id", 202L)
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getInvestmentsByInvestorId_Success() throws Exception {
        when(investmentService.getInvestmentsByInvestorId(202L))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/investments/investor")
                .header("X-User-Id", 202L)
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Investments fetched successfully"));
    }

    @Test
    void updateInvestmentStatus_Success() throws Exception {
        InvestmentStatusUpdateDto statusUpdate = new InvestmentStatusUpdateDto();
        statusUpdate.setStatus(ManualInvestmentStatus.APPROVED);
        responseDto.setStatus(InvestmentStatus.APPROVED);

        when(investmentService.updateInvestmentStatus(eq(1L), eq(5L), any(InvestmentStatusUpdateDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/investments/1/status")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Investment status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void getInvestmentById_Success() throws Exception {
        when(investmentService.getInvestmentById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/investments/1")
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Investment fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    void getInvestmentById_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(get("/investments/1")
                .header("X-User-Role", "ROLE_UNKNOWN"))
                .andExpect(status().is4xxClientError());
    }
}
