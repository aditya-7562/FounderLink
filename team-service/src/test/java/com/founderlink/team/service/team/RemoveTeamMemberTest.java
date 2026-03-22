package com.founderlink.team.service.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.TeamMemberNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.repository.TeamMemberRepository;
import com.founderlink.team.serviceImpl.TeamMemberServiceImpl;

@ExtendWith(MockitoExtension.class)
class RemoveTeamMemberTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private TeamMemberMapper teamMemberMapper;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private TeamMemberServiceImpl teamMemberService;

    private TeamMember teamMember;
    private StartupResponseDto startupResponseDto;

    @BeforeEach
    void setUp() {
        teamMember = new TeamMember();
        teamMember.setId(1L);
        teamMember.setStartupId(101L);
        teamMember.setUserId(202L);
        teamMember.setRole(TeamRole.CTO);
        teamMember.setIsActive(true);
        teamMember.setLeftAt(null);
        teamMember.setJoinedAt(LocalDateTime.now());

        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }

    // ─────────────────────────────────────────
    // SUCCESS — SOFT DELETE
    // ─────────────────────────────────────────
    @Test
    void removeTeamMember_Success() {

        // Arrange
        when(teamMemberRepository.findById(1L))
                .thenReturn(Optional.of(teamMember));
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);

        // Act
        teamMemberService.removeTeamMember(1L, 5L);

        // Assert soft delete
        assertThat(teamMember.getIsActive())
                .isFalse();
        assertThat(teamMember.getLeftAt())
                .isNotNull();

        // Verify save not delete
        verify(teamMemberRepository, times(1))
                .save(teamMember);
        verify(teamMemberRepository, never())
                .delete(any(TeamMember.class));
    }

    // MEMBER NOT FOUND
    
    @Test
    void removeTeamMember_NotFound_ThrowsException() {

        // Arrange
        when(teamMemberRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .removeTeamMember(999L, 5L))
                .isInstanceOf(
                        TeamMemberNotFoundException.class)
                .hasMessage(
                        "Team member not found with id: 999");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }

    // ─────────────────────────────────────────
    // STARTUP NOT FOUND
    // ─────────────────────────────────────────
    @Test
    void removeTeamMember_StartupNotFound_ThrowsException() {

        // Arrange
        when(teamMemberRepository.findById(1L))
                .thenReturn(Optional.of(teamMember));
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .removeTeamMember(1L, 5L))
                .isInstanceOf(
                        StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 101");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }

    // ─────────────────────────────────────────
    // NOT OWNER
    // ─────────────────────────────────────────
    @Test
    void removeTeamMember_NotOwner_ThrowsException() {

        // Arrange
        when(teamMemberRepository.findById(1L))
                .thenReturn(Optional.of(teamMember));
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .removeTeamMember(1L, 99L))
                .isInstanceOf(
                        ForbiddenAccessException.class)
                .hasMessage(
                        "You are not authorized to " +
                        "perform this action on this startup");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }

    // ─────────────────────────────────────────
    // FOUNDER REMOVING THEMSELVES
    // ─────────────────────────────────────────
    @Test
    void removeTeamMember_FounderRemovingThemselves_ThrowsException() {

        // Arrange
        teamMember.setUserId(5L);
        when(teamMemberRepository.findById(1L))
                .thenReturn(Optional.of(teamMember));
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .removeTeamMember(1L, 5L))
                .isInstanceOf(
                        UnauthorizedAccessException.class)
                .hasMessage(
                        "Founder cannot remove themselves " +
                        "from the team");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }
}