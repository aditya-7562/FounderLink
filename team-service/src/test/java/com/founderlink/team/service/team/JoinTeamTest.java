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
import com.founderlink.team.dto.request.JoinTeamRequestDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamMember;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.exception.AlreadyTeamMemberException;
import com.founderlink.team.exception.InvalidInvitationStatusException;
import com.founderlink.team.exception.InvitationNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.TeamMemberMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.repository.TeamMemberRepository;
import com.founderlink.team.serviceImpl.TeamMemberServiceImpl;

@ExtendWith(MockitoExtension.class)
class JoinTeamTest {

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

    private JoinTeamRequestDto requestDto;
    private Invitation invitation;
    private TeamMember teamMember;
    private TeamMemberResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = new JoinTeamRequestDto();
        requestDto.setInvitationId(1L);

        invitation = new Invitation();
        invitation.setId(1L);
        invitation.setStartupId(101L);
        invitation.setFounderId(5L);
        invitation.setInvitedUserId(202L);
        invitation.setRole(TeamRole.CTO);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreatedAt(LocalDateTime.now());

        teamMember = new TeamMember();
        teamMember.setId(1L);
        teamMember.setStartupId(101L);
        teamMember.setUserId(202L);
        teamMember.setRole(TeamRole.CTO);
        teamMember.setIsActive(true);
        teamMember.setLeftAt(null);
        teamMember.setJoinedAt(LocalDateTime.now());

        responseDto = new TeamMemberResponseDto();
        responseDto.setId(1L);
        responseDto.setStartupId(101L);
        responseDto.setUserId(202L);
        responseDto.setRole(TeamRole.CTO);
        responseDto.setIsActive(true);
        responseDto.setJoinedAt(LocalDateTime.now());
        responseDto.setLeftAt(null);
    }

    // SUCCESS

    @Test
    void joinTeam_Success() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));
        when(teamMemberRepository
                .existsByStartupIdAndUserIdAndIsActiveTrue(
                        101L, 202L))
                .thenReturn(false);
        when(teamMemberRepository
                .existsByStartupIdAndRoleAndIsActiveTrue(
                        101L, TeamRole.CTO))
                .thenReturn(false);
        when(teamMemberRepository
                .save(any(TeamMember.class)))
                .thenReturn(teamMember);
        when(invitationRepository
                .save(any(Invitation.class)))
                .thenReturn(invitation);
        when(teamMemberMapper.toResponseDto(teamMember))
                .thenReturn(responseDto);

        // Act
        TeamMemberResponseDto result =
                teamMemberService
                        .joinTeam(202L, requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStartupId())
                .isEqualTo(101L);
        assertThat(result.getUserId())
                .isEqualTo(202L);
        assertThat(result.getRole())
                .isEqualTo(TeamRole.CTO);
        assertThat(result.getIsActive())
                .isTrue();
        assertThat(result.getLeftAt())
                .isNull();

        verify(teamMemberRepository, times(1))
                .save(any(TeamMember.class));
        verify(invitationRepository, times(1))
                .save(any(Invitation.class));
    }

    // INVITATION NOT FOUND

    @Test
    void joinTeam_InvitationNotFound_ThrowsException() {

        // Arrange
        when(invitationRepository.findById(999L))
                .thenReturn(Optional.empty());
        requestDto.setInvitationId(999L);

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .joinTeam(202L, requestDto))
                .isInstanceOf(
                        InvitationNotFoundException.class)
                .hasMessage(
                        "Invitation not found with id: 999");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }
    
    // WRONG USER

    @Test
    void joinTeam_WrongUser_ThrowsException() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .joinTeam(99L, requestDto))
                .isInstanceOf(
                        UnauthorizedAccessException.class)
                .hasMessage(
                        "This invitation does not " +
                        "belong to you");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }

    // INVITATION NOT PENDING

    @Test
    void joinTeam_InvitationNotPending_ThrowsException() {

        // Arrange
        invitation.setStatus(
                InvitationStatus.REJECTED);
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .joinTeam(202L, requestDto))
                .isInstanceOf(
                        InvalidInvitationStatusException.class)
                .hasMessage(
                        "Only PENDING invitations " +
                        "can be accepted");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }

    // ALREADY ACTIVE MEMBER
 
    @Test
    void joinTeam_AlreadyActiveMember_ThrowsException() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));
        when(teamMemberRepository
                .existsByStartupIdAndUserIdAndIsActiveTrue(
                        101L, 202L))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .joinTeam(202L, requestDto))
                .isInstanceOf(
                        AlreadyTeamMemberException.class)
                .hasMessage(
                        "You are already a member " +
                        "of this startup");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }

    // ROLE ALREADY TAKEN

    @Test
    void joinTeam_RoleAlreadyTaken_ThrowsException() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));
        when(teamMemberRepository
                .existsByStartupIdAndUserIdAndIsActiveTrue(
                        101L, 202L))
                .thenReturn(false);
        when(teamMemberRepository
                .existsByStartupIdAndRoleAndIsActiveTrue(
                        101L, TeamRole.CTO))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                teamMemberService
                        .joinTeam(202L, requestDto))
                .isInstanceOf(
                        AlreadyTeamMemberException.class)
                .hasMessage(
                        "This role is already filled " +
                        "in the team");

        verify(teamMemberRepository, never())
                .save(any(TeamMember.class));
    }

    // INVITATION UPDATED TO ACCEPTED
    
    @Test
    void joinTeam_Success_InvitationUpdatedToAccepted() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));
        when(teamMemberRepository
                .existsByStartupIdAndUserIdAndIsActiveTrue(
                        101L, 202L))
                .thenReturn(false);
        when(teamMemberRepository
                .existsByStartupIdAndRoleAndIsActiveTrue(
                        101L, TeamRole.CTO))
                .thenReturn(false);
        when(teamMemberRepository
                .save(any(TeamMember.class)))
                .thenReturn(teamMember);
        when(invitationRepository
                .save(any(Invitation.class)))
                .thenReturn(invitation);
        when(teamMemberMapper.toResponseDto(teamMember))
                .thenReturn(responseDto);


        teamMemberService.joinTeam(202L, requestDto);

        assertThat(invitation.getStatus())
                .isEqualTo(InvitationStatus.ACCEPTED);

        verify(invitationRepository, times(1))
                .save(invitation);
    }
}