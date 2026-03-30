package com.founderlink.team.service.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.command.TeamMemberCommandService;
import com.founderlink.team.dto.request.JoinTeamRequestDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.events.TeamMemberAcceptedEvent;
import com.founderlink.team.exception.AlreadyTeamMemberException;
import com.founderlink.team.exception.InvalidInvitationStatusException;
import com.founderlink.team.exception.InvitationNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.repository.TeamMemberRepository;

@ExtendWith(MockitoExtension.class)
class JoinTeamTest {

    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private TeamMemberMapper teamMemberMapper;
    @Mock private StartupServiceClient startupServiceClient;
    @Mock private TeamEventPublisher teamEventPublisher;

    @InjectMocks
    private TeamMemberCommandService teamMemberCommandService;

    private Invitation invitation;
    private TeamMember teamMember;
    private TeamMemberResponseDto responseDto;
    private JoinTeamRequestDto requestDto;

    @BeforeEach
    void setUp() {
        invitation = new Invitation();
        invitation.setId(1L);
        invitation.setStartupId(101L);
        invitation.setFounderId(5L);
        invitation.setInvitedUserId(300L);
        invitation.setRole(TeamRole.CTO);
        invitation.setStatus(InvitationStatus.PENDING);

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

        requestDto = new JoinTeamRequestDto();
        requestDto.setInvitationId(1L);
    }

    @Test
    void joinTeam_Success() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(teamMemberRepository.existsByStartupIdAndUserIdAndIsActiveTrue(101L, 300L)).thenReturn(false);
        when(teamMemberRepository.existsByStartupIdAndRoleAndIsActiveTrue(101L, TeamRole.CTO)).thenReturn(false);
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(teamMember);
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        when(teamMemberMapper.toResponseDto(teamMember)).thenReturn(responseDto);

        TeamMemberResponseDto result = teamMemberCommandService.joinTeam(300L, requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getStartupId()).isEqualTo(101L);
        assertThat(result.getUserId()).isEqualTo(300L);
        verify(teamMemberRepository, times(1)).save(any(TeamMember.class));
        verify(invitationRepository, times(1)).save(any(Invitation.class));
        verify(teamEventPublisher, times(1)).publishTeamMemberAcceptedEvent(any(TeamMemberAcceptedEvent.class));
    }

    @Test
    void joinTeam_InvitationNotFound_ThrowsException() {
        requestDto.setInvitationId(99L);
        when(invitationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamMemberCommandService.joinTeam(300L, requestDto))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void joinTeam_WrongUser_ThrowsException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> teamMemberCommandService.joinTeam(999L, requestDto))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("This invitation does not belong to you");
    }

    @Test
    void joinTeam_NotPendingInvitation_ThrowsException() {
        invitation.setStatus(InvitationStatus.CANCELLED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> teamMemberCommandService.joinTeam(300L, requestDto))
                .isInstanceOf(InvalidInvitationStatusException.class)
                .hasMessage("Only PENDING invitations can be accepted");
    }

    @Test
    void joinTeam_AlreadyMember_ThrowsException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(teamMemberRepository.existsByStartupIdAndUserIdAndIsActiveTrue(101L, 300L)).thenReturn(true);

        assertThatThrownBy(() -> teamMemberCommandService.joinTeam(300L, requestDto))
                .isInstanceOf(AlreadyTeamMemberException.class)
                .hasMessage("You are already a member of this startup");
    }

    @Test
    void joinTeam_RoleAlreadyFilled_ThrowsException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(teamMemberRepository.existsByStartupIdAndUserIdAndIsActiveTrue(101L, 300L)).thenReturn(false);
        when(teamMemberRepository.existsByStartupIdAndRoleAndIsActiveTrue(101L, TeamRole.CTO)).thenReturn(true);

        assertThatThrownBy(() -> teamMemberCommandService.joinTeam(300L, requestDto))
                .isInstanceOf(AlreadyTeamMemberException.class)
                .hasMessage("This role is already filled in the team");
    }
}
