package com.founderlink.investment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.entity.ManualInvestmentStatus;
import com.founderlink.investment.service.InvestmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    // --- Create Investment Tests ---

    @Test
    @DisplayName("Create Investment - Success")
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
    @DisplayName("Create Investment - Wrong Role throws Forbidden")
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

    // --- Get Investments by Startup ID ---

    @Test
    @DisplayName("Get by Startup ID - Success (Unpaginated)")
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
    @DisplayName("Get by Startup ID - Success (Paginated Admin)")
    void getInvestmentsByStartupId_Paginated_Success() throws Exception {
        when(investmentService.getInvestmentsByStartupId(eq(101L), eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDto)));

        mockMvc.perform(get("/investments/startup/101")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].startupId").value(101L))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1));
    }

    @Test
    @DisplayName("Get by Startup ID - Paginated with complex sort")
    void getInvestmentsByStartupId_Paginated_ComplexSort() throws Exception {
        when(investmentService.getInvestmentsByStartupId(eq(101L), eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDto)));

        mockMvc.perform(get("/investments/startup/101")
                .param("page", "0")
                .param("sort", " amount , asc ")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Get by Startup ID - Paginated with empty sort")
    void getInvestmentsByStartupId_Paginated_EmptySort() throws Exception {
        when(investmentService.getInvestmentsByStartupId(eq(101L), eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDto)));

        mockMvc.perform(get("/investments/startup/101")
                .param("page", "-1") // tests safePage Math.max
                .param("size", "100") // tests safeSize Math.min
                .param("sort", "")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get by Startup ID - Paginated with missing sort param")
    void getInvestmentsByStartupId_Paginated_NoSort() throws Exception {
        when(investmentService.getInvestmentsByStartupId(eq(101L), eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDto)));

        mockMvc.perform(get("/investments/startup/101")
                .param("page", "0")
                // no sort param
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get by Startup ID - Paginated with small size")
    void getInvestmentsByStartupId_Paginated_SmallSize() throws Exception {
        when(investmentService.getInvestmentsByStartupId(eq(101L), eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDto)));

        mockMvc.perform(get("/investments/startup/101")
                .param("page", "0")
                .param("size", "0") // tests Math.max(size, 1)
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get by Startup ID - Paginated with comma sort")
    void getInvestmentsByStartupId_Paginated_CommaSort() throws Exception {
        when(investmentService.getInvestmentsByStartupId(eq(101L), eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDto)));

        mockMvc.perform(get("/investments/startup/101")
                .param("sort", ",desc") // tests tokens[0].isBlank()
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get by Startup ID - Wrong Role Forbidden")
    void getInvestmentsByStartupId_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(get("/investments/startup/101")
                .header("X-User-Id", 202L)
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().is4xxClientError());
    }

    // --- Get Investments by Investor ID ---

    @Test
    @DisplayName("Get by Investor ID - Success (Unpaginated)")
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
    @DisplayName("Get by Investor ID - Success (Paginated Admin)")
    void getInvestmentsByInvestorId_Paginated_Success() throws Exception {
        when(investmentService.getInvestmentsByInvestorId(eq(202L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(responseDto)));

        mockMvc.perform(get("/investments/investor")
                .param("page", "0")
                .header("X-User-Id", 202L)
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].investorId").value(202L));
    }

    @Test
    @DisplayName("Get by Investor ID - Wrong Role Forbidden")
    void getInvestmentsByInvestorId_Forbidden() throws Exception {
        mockMvc.perform(get("/investments/investor")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().is4xxClientError());
    }

    // --- Update Investment Status ---

    @Test
    @DisplayName("Update Investment Status - Success")
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
    @DisplayName("Update Investment Status - Wrong Role Forbidden")
    void updateInvestmentStatus_Forbidden() throws Exception {
        InvestmentStatusUpdateDto statusUpdate = new InvestmentStatusUpdateDto();
        statusUpdate.setStatus(ManualInvestmentStatus.APPROVED);

        mockMvc.perform(put("/investments/1/status")
                .header("X-User-Id", 202L)
                .header("X-User-Role", "ROLE_INVESTOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().is4xxClientError());
    }

    // --- Get Investment By ID ---

    @Test
    @DisplayName("Get Investment By ID - Success (Founder)")
    void getInvestmentById_Success_Founder() throws Exception {
        when(investmentService.getInvestmentById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/investments/1")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("Get Investment By ID - Success (Investor)")
    void getInvestmentById_Success_Investor() throws Exception {
        when(investmentService.getInvestmentById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/investments/1")
                .header("X-User-Id", 202L) // Matches investorId 202L
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("Get Investment By ID - Success (Admin)")
    void getInvestmentById_Success_Admin() throws Exception {
        when(investmentService.getInvestmentById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/investments/1")
                .header("X-User-Id", 999L) // Admin bypasses ID check
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("Get Investment By ID - Unowned Investor Forbidden")
    void getInvestmentById_InvestorNotOwner_Forbidden() throws Exception {
        when(investmentService.getInvestmentById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/investments/1")
                .header("X-User-Id", 303L) // Does NOT match investorId 202L
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Get Investment By ID - Unknown Role Forbidden")
    void getInvestmentById_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(get("/investments/1")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_UNKNOWN"))
                .andExpect(status().is4xxClientError());
    }
}
