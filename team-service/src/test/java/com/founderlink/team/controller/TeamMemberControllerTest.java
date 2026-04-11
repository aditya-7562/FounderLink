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
    void getTeamByStartupId_Paginated_Success() throws Exception {
        org.springframework.data.domain.Page<TeamMemberResponseDto> page = new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(teamMemberService.getTeamByStartupId(eq(101L), eq(5L), eq("ROLE_FOUNDER"), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/teams/startup/101")
                .header("X-User-Id", 5L)
                .header("X-User-Role", "ROLE_FOUNDER")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].startupId").value(101L))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getTeamByStartupId_AsInvestor_Success() throws Exception {
        when(teamMemberService.getTeamByStartupId(101L, 7L, "ROLE_INVESTOR"))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/teams/startup/101")
                .header("X-User-Id", 7L)
                .header("X-User-Role", "ROLE_INVESTOR"))
                .andExpect(status().isOk());
    }

    @Test
    void getTeamByStartupId_AsAdmin_Success() throws Exception {
        when(teamMemberService.getTeamByStartupId(101L, 99L, "ROLE_ADMIN"))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/teams/startup/101")
                .header("X-User-Id", 99L)
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void getTeamByStartupId_InvalidRole_Forbidden() throws Exception {
        mockMvc.perform(get("/teams/startup/101")
                .header("X-User-Id", 123L)
                .header("X-User-Role", "ROLE_WRONG"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMemberHistory_Paginated_Success() throws Exception {
        org.springframework.data.domain.Page<TeamMemberResponseDto> page = new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(teamMemberService.getMemberHistory(eq(300L), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(300L));
    }

    @Test
    void getActiveMemberRoles_Paginated_Success() throws Exception {
        org.springframework.data.domain.Page<TeamMemberResponseDto> page = new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(teamMemberService.getActiveMemberRoles(eq(300L), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/teams/member/active")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(300L));
    }

    @Test
    void resolveSort_Exhaustive() throws Exception {
        org.springframework.data.domain.Page<TeamMemberResponseDto> page = new org.springframework.data.domain.PageImpl<>(List.of(responseDto));
        when(teamMemberService.getMemberHistory(any(), any())).thenReturn(page);

        // Case: sort is null
        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0"))
                .andExpect(status().isOk());

        // Case: sort=id (tokens.length == 1)
        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0")
                .param("sort", "id"))
                .andExpect(status().isOk());
                
        // Case: sort=id,asc
        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0")
                .param("sort", "id,asc"))
                .andExpect(status().isOk());

        // Case: sort=,desc
        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER")
                .param("page", "0")
                .param("sort", ",desc"))
                .andExpect(status().isOk());
    }

    @Test
    void rbac_MissingRoleBranches() throws Exception {
        // Test ROLE_ADMIN branch for getMemberHistory
        when(teamMemberService.getMemberHistory(99L)).thenReturn(List.of());
        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 99L)
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());

        // Test ROLE_ADMIN branch for getActiveMemberRoles
        when(teamMemberService.getActiveMemberRoles(99L)).thenReturn(List.of());
        mockMvc.perform(get("/teams/member/active")
                .header("X-User-Id", 99L)
                .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void isPaginated_False_Check() throws Exception {
        // Trigger non-paginated path in getMemberHistory
        when(teamMemberService.getMemberHistory(300L)).thenReturn(List.of());
        mockMvc.perform(get("/teams/member/history")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk());

        // Trigger non-paginated path in getActiveMemberRoles
        when(teamMemberService.getActiveMemberRoles(300L)).thenReturn(List.of());
        mockMvc.perform(get("/teams/member/active")
                .header("X-User-Id", 300L)
                .header("X-User-Role", "ROLE_COFOUNDER"))
                .andExpect(status().isOk());
    }
}
