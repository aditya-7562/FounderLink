package com.founderlink.team.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.service.InvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = InvitationController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvitationService invitationService;

    @Autowired
    private ObjectMapper objectMapper;

    private InvitationResponseDto responseDto;
    private InvitationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        responseDto = new InvitationResponseDto();
        responseDto.setId(1L);
        responseDto.setStartupId(101L);
        responseDto.setInvitedUserId(300L);
        responseDto.setStatus(InvitationStatus.PENDING);

        requestDto = new InvitationRequestDto();
        requestDto.setStartupId(101L);
        requestDto.setInvitedUserId(300L);
        requestDto.setRole(TeamRole.CTO);
    }

    @Test
    void sendInvitation_Success() throws Exception {
        when(invitationService.sendInvitation(eq(5L), any(InvitationRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/teams/invite")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Invitation sent successfully"))
                .andExpect(jsonPath("$.data.startupId").value(101L));
    }

    @Test
    void sendInvitation_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(post("/teams/invite")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void cancelInvitation_Success() throws Exception {
        responseDto.setStatus(InvitationStatus.CANCELLED);
        when(invitationService.cancelInvitation(1L, 5L)).thenReturn(responseDto);

        mockMvc.perform(put("/teams/invitations/1/cancel")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation cancelled successfully"));
    }

    @Test
    void cancelInvitation_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(put("/teams/invitations/1/cancel")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void rejectInvitation_Success() throws Exception {
        responseDto.setStatus(InvitationStatus.REJECTED);
        when(invitationService.rejectInvitation(1L, 300L)).thenReturn(responseDto);

        mockMvc.perform(put("/teams/invitations/1/reject")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation rejected successfully"));
    }

    @Test
    void getInvitationsByUserId_Success() throws Exception {
        when(invitationService.getInvitationsByUserId(300L)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/teams/invitations/user")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitations fetched successfully"))
                .andExpect(jsonPath("$.data[0].invitedUserId").value(300L));
    }

    @Test
    void getInvitationsByUserId_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(get("/teams/invitations/user")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getInvitationsByUserId_Paginated_Success() throws Exception {
        org.springframework.data.domain.Page<InvitationResponseDto> page = new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(invitationService.getInvitationsByUserId(eq(300L), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/teams/invitations/user")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].invitedUserId").value(300L))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getInvitationsByStartupId_Paginated_Success() throws Exception {
        org.springframework.data.domain.Page<InvitationResponseDto> page = new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(invitationService.getInvitationsByStartupId(eq(101L), eq(5L), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/teams/invitations/startup/101")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].startupId").value(101L));
    }

    @Test
    void resolveSort_Exhaustive() throws Exception {
        org.springframework.data.domain.Page<InvitationResponseDto> page = new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(invitationService.getInvitationsByUserId(any(), any())).thenReturn(page);

        // Case: sort is null
        mockMvc.perform(get("/teams/invitations/user")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0"))
                .andExpect(status().isOk());

        // Case: sort=id (tokens.length == 1)
        mockMvc.perform(get("/teams/invitations/user")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0")
                .param("sort", "id"))
                .andExpect(status().isOk());

        // Case: sort=id,asc (tokens.length > 1, but NOT desc)
        mockMvc.perform(get("/teams/invitations/user")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0")
                .param("sort", "id,asc"))
                .andExpect(status().isOk());

        // Case: non-paginated path check
        when(invitationService.getInvitationsByUserId(300L)).thenReturn(List.of(responseDto));
        mockMvc.perform(get("/teams/invitations/user")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk());
                
        // Case: non-paginated path for startup
        when(invitationService.getInvitationsByStartupId(101L, 5L)).thenReturn(List.of(responseDto));
        mockMvc.perform(get("/teams/invitations/startup/101")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk());
    }
}
