package com.founderlink.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.founderlink.team.command.TeamMemberCommandService;
import com.founderlink.team.dto.request.JoinTeamRequestDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.query.TeamMemberQueryService;
import com.founderlink.team.serviceImpl.TeamMemberServiceImpl;

@ExtendWith(MockitoExtension.class)
class TeamMemberServiceImplTest {

    @Mock
    private TeamMemberCommandService commandService;

    @Mock
    private TeamMemberQueryService queryService;

    @InjectMocks
    private TeamMemberServiceImpl teamMemberService;

    @Test
    void joinTeam_delegatesToCommandService() {
        JoinTeamRequestDto requestDto = new JoinTeamRequestDto();
        TeamMemberResponseDto responseDto = new TeamMemberResponseDto();
        when(commandService.joinTeam(300L, requestDto)).thenReturn(responseDto);

        TeamMemberResponseDto result = teamMemberService.joinTeam(300L, requestDto);

        assertThat(result).isSameAs(responseDto);
        verify(commandService).joinTeam(300L, requestDto);
    }

    @Test
    void removeTeamMember_delegatesToCommandService() {
        doNothing().when(commandService).removeTeamMember(1L, 5L);

        teamMemberService.removeTeamMember(1L, 5L);

        verify(commandService).removeTeamMember(1L, 5L);
    }

    @Test
    void getTeamByStartupId_delegatesToQueryService() {
        List<TeamMemberResponseDto> list = List.of(new TeamMemberResponseDto());
        when(queryService.getTeamByStartupId(101L, 5L, "ROLE_FOUNDER")).thenReturn(list);

        List<TeamMemberResponseDto> result = teamMemberService.getTeamByStartupId(101L, 5L, "ROLE_FOUNDER");

        assertThat(result).isSameAs(list);
        verify(queryService).getTeamByStartupId(101L, 5L, "ROLE_FOUNDER");
    }

    @Test
    void getTeamByStartupId_pageable_delegatesToQueryService() {
        Page<TeamMemberResponseDto> page = new PageImpl<>(List.of());
        Pageable pageable = Pageable.unpaged();
        when(queryService.getTeamByStartupId(101L, 5L, "ROLE_FOUNDER", pageable)).thenReturn(page);

        Page<TeamMemberResponseDto> result = teamMemberService.getTeamByStartupId(101L, 5L, "ROLE_FOUNDER", pageable);

        assertThat(result).isSameAs(page);
        verify(queryService).getTeamByStartupId(101L, 5L, "ROLE_FOUNDER", pageable);
    }

    @Test
    void getMemberHistory_delegatesToQueryService() {
        List<TeamMemberResponseDto> list = List.of(new TeamMemberResponseDto());
        when(queryService.getMemberHistory(300L)).thenReturn(list);

        List<TeamMemberResponseDto> result = teamMemberService.getMemberHistory(300L);

        assertThat(result).isSameAs(list);
        verify(queryService).getMemberHistory(300L);
    }

    @Test
    void getMemberHistory_pageable_delegatesToQueryService() {
        Page<TeamMemberResponseDto> page = new PageImpl<>(List.of());
        Pageable pageable = Pageable.unpaged();
        when(queryService.getMemberHistory(300L, pageable)).thenReturn(page);

        Page<TeamMemberResponseDto> result = teamMemberService.getMemberHistory(300L, pageable);

        assertThat(result).isSameAs(page);
        verify(queryService).getMemberHistory(300L, pageable);
    }

    @Test
    void getActiveMemberRoles_delegatesToQueryService() {
        List<TeamMemberResponseDto> list = List.of(new TeamMemberResponseDto());
        when(queryService.getActiveMemberRoles(300L)).thenReturn(list);

        List<TeamMemberResponseDto> result = teamMemberService.getActiveMemberRoles(300L);

        assertThat(result).isSameAs(list);
        verify(queryService).getActiveMemberRoles(300L);
    }

    @Test
    void getActiveMemberRoles_pageable_delegatesToQueryService() {
        Page<TeamMemberResponseDto> page = new PageImpl<>(List.of());
        Pageable pageable = Pageable.unpaged();
        when(queryService.getActiveMemberRoles(300L, pageable)).thenReturn(page);

        Page<TeamMemberResponseDto> result = teamMemberService.getActiveMemberRoles(300L, pageable);

        assertThat(result).isSameAs(page);
        verify(queryService).getActiveMemberRoles(300L, pageable);
    }

    @Test
    void isTeamMember_delegatesToQueryService() {
        when(queryService.isTeamMember(101L, 300L)).thenReturn(true);

        boolean result = teamMemberService.isTeamMember(101L, 300L);

        assertThat(result).isTrue();
        verify(queryService).isTeamMember(101L, 300L);
    }
}
