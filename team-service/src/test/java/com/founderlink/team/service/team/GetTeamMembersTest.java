package com.founderlink.team.service.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private TeamMemberQueryService teamMemberService;

    private TeamMember teamMember1;
    private TeamMember teamMember2;
    private TeamMemberResponseDto responseDto1;
    private TeamMemberResponseDto responseDto2;
    private StartupResponseDto startupResponseDto;

    @BeforeEach
    void setUp() {
        teamMember1 = new TeamMember();
        teamMember1.setId(1L);
        teamMember1.setStartupId(101L);
        teamMember1.setUserId(202L);
        teamMember1.setRole(TeamRole.CTO);
        teamMember1.setIsActive(true);
        teamMember1.setLeftAt(null);
        teamMember1.setJoinedAt(LocalDateTime.now());

        teamMember2 = new TeamMember();
        teamMember2.setId(2L);
        teamMember2.setStartupId(101L);
        teamMember2.setUserId(303L);
        teamMember2.setRole(TeamRole.CPO);
        teamMember2.setIsActive(true);
        teamMember2.setLeftAt(null);
        teamMember2.setJoinedAt(LocalDateTime.now());

        responseDto1 = new TeamMemberResponseDto();
        responseDto1.setId(1L);
        responseDto1.setStartupId(101L);
        responseDto1.setUserId(202L);
        responseDto1.setRole(TeamRole.CTO);
        responseDto1.setIsActive(true);
        responseDto1.setJoinedAt(LocalDateTime.now());
        responseDto1.setLeftAt(null);

        responseDto2 = new TeamMemberResponseDto();
        responseDto2.setId(2L);
        responseDto2.setStartupId(101L);
        responseDto2.setUserId(303L);
        responseDto2.setRole(TeamRole.CPO);
        responseDto2.setIsActive(true);
        responseDto2.setJoinedAt(LocalDateTime.now());
        responseDto2.setLeftAt(null);

        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }

    // ─────────────────────────────────────────
    // SUCCESS — FOUNDER
    // ─────────────────────────────────────────
    @Test
    void getTeamByStartupId_Founder_Success() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(teamMemberRepository
                .findByStartupIdAndIsActiveTrue(101L))
                .thenReturn(
                        List.of(teamMember1,
                                teamMember2));
        when(teamMemberMapper.toResponseDto(teamMember1))
                .thenReturn(responseDto1);
        when(teamMemberMapper.toResponseDto(teamMember2))
                .thenReturn(responseDto2);

        // Act
        List<TeamMemberResponseDto> result =
                teamMemberService.getTeamByStartupId(
                        101L, 5L, "ROLE_FOUNDER");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole())
                .isEqualTo(TeamRole.CTO);
        assertThat(result.get(1).getRole())
                .isEqualTo(TeamRole.CPO);
    }

    // ─────────────────────────────────────────
    // SUCCESS — INVESTOR
    // No FeignClient needed
    // ─────────────────────────────────────────
    @Test
    void getTeamByStartupId_Investor_Success() {

        // Arrange
        when(teamMemberRepository
                .findByStartupIdAndIsActiveTrue(101L))
                .thenReturn(List.of(teamMember1));
        when(teamMemberMapper.toResponseDto(teamMember1))
                .thenReturn(responseDto1);

        // Act
        List<TeamMemberResponseDto> result =
                teamMemberService.getTeamByStartupId(
                        101L, 10L, "ROLE_INVESTOR");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
    }

    // ─────────────────────────────────────────
    // FOUNDER NOT OWNER
    // ─────────────────────────────────────────
    @Test
    void getTeamByStartupId_NotOwner_ThrowsException() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService.getTeamByStartupId(
                        101L, 99L, "ROLE_FOUNDER"))
                .isInstanceOf(
                        ForbiddenAccessException.class)
                .hasMessage(
                        "You are not authorized to " +
                        "perform this action on this startup");
    }

    // ─────────────────────────────────────────
    // STARTUP NOT FOUND
    // ─────────────────────────────────────────
    @Test
    void getTeamByStartupId_StartupNotFound_ThrowsException() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService.getTeamByStartupId(
                        101L, 5L, "ROLE_FOUNDER"))
                .isInstanceOf(
                        StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 101");
    }

    // ─────────────────────────────────────────
    // EMPTY TEAM
    // ─────────────────────────────────────────
    @Test
    void getTeamByStartupId_EmptyTeam_ReturnsEmpty() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(teamMemberRepository
                .findByStartupIdAndIsActiveTrue(101L))
                .thenReturn(List.of());

        // Act
        List<TeamMemberResponseDto> result =
                teamMemberService.getTeamByStartupId(
                        101L, 5L, "ROLE_FOUNDER");

        // Assert
        assertThat(result).isEmpty();
    }
}