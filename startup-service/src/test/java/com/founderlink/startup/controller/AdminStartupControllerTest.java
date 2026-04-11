package com.founderlink.startup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.startup.dto.UpdateModerationRequest;
import com.founderlink.startup.entity.ModerationStatus;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.repository.StartupRepository;

import java.util.Optional;

@WebMvcTest(value = AdminStartupController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ExtendWith(MockitoExtension.class)
class AdminStartupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StartupRepository startupRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Startup startup;

    @BeforeEach
    void setUp() {
        startup = new Startup();
        startup.setId(1L);
        startup.setName("EduReach");
        startup.setFounderId(5L);
        startup.setStage(StartupStage.MVP);
        startup.setIsDeleted(false);
    }

    @Test
    void updateModerationStatus_AsAdmin_Returns200() throws Exception {
        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));
        when(startupRepository.save(any(Startup.class))).thenReturn(startup);

        UpdateModerationRequest request = new UpdateModerationRequest();
        request.setStatus(ModerationStatus.APPROVED);
        request.setReason("Looks good");

        mockMvc.perform(put("/startup/admin/1/moderation")
                .header("X-User-Role", "ROLE_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Moderation status updated"));
    }

    @Test
    void updateModerationStatus_NotAdmin_Returns403() throws Exception {
        UpdateModerationRequest request = new UpdateModerationRequest();
        request.setStatus(ModerationStatus.APPROVED);
        request.setReason("Looks good");

        mockMvc.perform(put("/startup/admin/1/moderation")
                .header("X-User-Role", "ROLE_FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteStartup_AsAdmin_Returns200() throws Exception {
        when(startupRepository.findById(1L)).thenReturn(Optional.of(startup));
        when(startupRepository.save(any(Startup.class))).thenReturn(startup);

        mockMvc.perform(delete("/startup/admin/1")
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Startup successfully deleted"));
    }

    @Test
    void deleteStartup_NotAdmin_Returns403() throws Exception {
        mockMvc.perform(delete("/startup/admin/1")
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateModerationStatus_StartupNotFound_Returns404() throws Exception {
        when(startupRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateModerationRequest request = new UpdateModerationRequest();
        request.setStatus(ModerationStatus.REJECTED);
        request.setReason("Not a real startup");

        mockMvc.perform(put("/startup/admin/99/moderation")
                .header("X-User-Role", "ROLE_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStartup_StartupNotFound_Returns404() throws Exception {
        when(startupRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/startup/admin/99")
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isNotFound());
    }
}
