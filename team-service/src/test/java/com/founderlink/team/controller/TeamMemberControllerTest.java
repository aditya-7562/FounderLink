package com.founderlink.team.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.team.dto.request.JoinTeamRequestDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.service.TeamMemberService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TeamMemberController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class TeamMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamMemberService teamMemberService;

    @Autowired
    private ObjectMapper objectMapper;

    private TeamMemberResponseDto responseDto;

    @BeforeEach
    void setUp() {
        responseDto = new TeamMemberResponseDto();
        responseDto.setId(1L);
        responseDto.setStartupId(101L);
        responseDto.setUserId(300L);
        responseDto.setRole(TeamRole.CTO);
    }

    @Test
    void joinTeam_Success() throws Exception {
        JoinTeamRequestDto request = new JoinTeamRequestDto();
        request.setInvitationId(1L);

        when(teamMemberService.joinTeam(eq(300L), any(JoinTeamRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/teams/join")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Successfully joined the team"))
                .andExpect(jsonPath("$.data.startupId").value(101L));
    }

    @Test
    void joinTeam_WrongRole_Forbidden() throws Exception {
        JoinTeamRequestDto request = new JoinTeamRequestDto();
        request.setInvitationId(1L);

        mockMvc.perform(post("/teams/join")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getTeamByStartupId_AsFounder_Success() throws Exception {
        when(teamMemberService.getTeamByStartupId(101L, 5L, "ROLE_FOUNDER"))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/teams/startup/101")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Team members fetched successfully"))
                .andExpect(jsonPath("$.data[0].startupId").value(101L));
    }

    @Test
    void getTeamByStartupId_AsCofounder_NotMember_Forbidden() throws Exception {
        when(teamMemberService.isTeamMember(101L, 300L)).thenReturn(false);

        mockMvc.perform(get("/teams/startup/101")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void removeTeamMember_Success() throws Exception {
        doNothing().when(teamMemberService).removeTeamMember(1L, 5L);

        mockMvc.perform(delete("/teams/1")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Team member removed successfully"));
    }

    @Test
    void removeTeamMember_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(delete("/teams/1")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMemberHistory_Success() throws Exception {
        when(teamMemberService.getMemberHistory(300L)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Member history fetched successfully"));
    }

    @Test
    void getMemberHistory_WrongRole_Forbidden() throws Exception {
        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getActiveMemberRoles_Success() throws Exception {
        when(teamMemberService.getActiveMemberRoles(300L)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/teams/member/active")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Active roles fetched successfully"));
    }
}
