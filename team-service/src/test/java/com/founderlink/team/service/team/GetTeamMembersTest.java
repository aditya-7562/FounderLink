package com.founderlink.team.service.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.query.TeamMemberQueryService;
import com.founderlink.team.repository.TeamMemberRepository;

@ExtendWith(MockitoExtension.class)
class GetTeamMembersTest {

    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TeamMemberMapper teamMemberMapper;
    @Mock private StartupServiceClient startupServiceClient;

    @InjectMocks
    private TeamMemberQueryService teamMemberQueryService;

    private TeamMember teamMember;
    private TeamMemberResponseDto responseDto;
    private StartupResponseDto startupResponseDto;

    @BeforeEach
    void setUp() {
        teamMember = new TeamMember();
        teamMember.setId(1L);
        teamMember.setStartupId(101L);
        teamMember.setUserId(300L);
        teamMember.setRole(TeamRole.CTO);

        responseDto = new TeamMemberResponseDto();
        responseDto.setId(1L);
        responseDto.setStartupId(101L);
        responseDto.setUserId(300L);
        responseDto.setRole(TeamRole.CTO);

        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }

    @Test
    void getTeamByStartupId_AsFounder_Success() {
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);
        when(teamMemberRepository.findByStartupIdAndIsActiveTrue(101L)).thenReturn(List.of(teamMember));
        when(teamMemberMapper.toResponseDto(teamMember)).thenReturn(responseDto);

        List<TeamMemberResponseDto> result = teamMemberQueryService.getTeamByStartupId(101L, 5L, "ROLE_FOUNDER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartupId()).isEqualTo(101L);
    }

    @Test
    void getTeamByStartupId_AsInvestor_Success() {
        when(teamMemberRepository.findByStartupIdAndIsActiveTrue(101L)).thenReturn(List.of(teamMember));
        when(teamMemberMapper.toResponseDto(teamMember)).thenReturn(responseDto);

        List<TeamMemberResponseDto> result = teamMemberQueryService.getTeamByStartupId(101L, 300L, "ROLE_INVESTOR");

        assertThat(result).hasSize(1);
    }

    @Test
    void getTeamByStartupId_StartupNotFound_ThrowsException() {
        when(startupServiceClient.getStartupById(101L)).thenReturn(null);

        assertThatThrownBy(() -> teamMemberQueryService.getTeamByStartupId(101L, 5L, "ROLE_FOUNDER"))
                .isInstanceOf(StartupNotFoundException.class);
    }

    @Test
    void getTeamByStartupId_NotFounder_ThrowsException() {
        startupResponseDto.setFounderId(99L);
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);

        assertThatThrownBy(() -> teamMemberQueryService.getTeamByStartupId(101L, 5L, "ROLE_FOUNDER"))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void getMemberHistory_Success() {
        when(teamMemberRepository.findByUserId(300L)).thenReturn(List.of(teamMember));
        when(teamMemberMapper.toResponseDto(teamMember)).thenReturn(responseDto);

        List<TeamMemberResponseDto> result = teamMemberQueryService.getMemberHistory(300L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(300L);
    }

    @Test
    void getActiveMemberRoles_Success() {
        when(teamMemberRepository.findByUserIdAndIsActiveTrue(300L)).thenReturn(List.of(teamMember));
        when(teamMemberMapper.toResponseDto(teamMember)).thenReturn(responseDto);

        List<TeamMemberResponseDto> result = teamMemberQueryService.getActiveMemberRoles(300L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(TeamRole.CTO);
    }

    @Test
    void isTeamMember_True() {
        when(teamMemberRepository.existsByStartupIdAndUserIdAndIsActiveTrue(101L, 300L)).thenReturn(true);

        assertThat(teamMemberQueryService.isTeamMember(101L, 300L)).isTrue();
    }

    @Test
    void isTeamMember_False() {
        when(teamMemberRepository.existsByStartupIdAndUserIdAndIsActiveTrue(101L, 999L)).thenReturn(false);

        assertThat(teamMemberQueryService.isTeamMember(101L, 999L)).isFalse();
    }
}
