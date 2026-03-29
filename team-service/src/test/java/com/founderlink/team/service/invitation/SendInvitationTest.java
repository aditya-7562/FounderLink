package com.founderlink.team.service.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.command.InvitationCommandService;
import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.events.TeamInviteEvent;
import com.founderlink.team.exception.DuplicateInvitationException;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class SendInvitationTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private InvitationMapper invitationMapper;
    @Mock private TeamEventPublisher eventPublisher;
    @Mock private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvitationCommandService invitationCommandService;

    private InvitationRequestDto requestDto;
    private Invitation invitation;
    private InvitationResponseDto responseDto;
    private StartupResponseDto startupResponseDto;

    @BeforeEach
    void setUp() {
        requestDto = new InvitationRequestDto();
        requestDto.setStartupId(101L);
        requestDto.setInvitedUserId(300L);
        requestDto.setRole(TeamRole.CTO);

        invitation = new Invitation();
        invitation.setId(1L);
        invitation.setStartupId(101L);
        invitation.setFounderId(5L);
        invitation.setInvitedUserId(300L);
        invitation.setRole(TeamRole.CTO);
        invitation.setStatus(InvitationStatus.PENDING);

        responseDto = new InvitationResponseDto();
        responseDto.setId(1L);
        responseDto.setStartupId(101L);
        responseDto.setInvitedUserId(300L);
        responseDto.setStatus(InvitationStatus.PENDING);

        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }

    @Test
    void sendInvitation_Success() {
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);
        when(invitationRepository.existsByStartupIdAndInvitedUserIdAndStatus(
                101L, 300L, InvitationStatus.PENDING)).thenReturn(false);
        when(invitationRepository.existsByStartupIdAndRoleAndStatus(
                101L, TeamRole.CTO, InvitationStatus.PENDING)).thenReturn(false);
        when(invitationMapper.toEntity(requestDto, 5L)).thenReturn(invitation);
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        when(invitationMapper.toResponseDto(invitation)).thenReturn(responseDto);

        InvitationResponseDto result = invitationCommandService.sendInvitation(5L, requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getStartupId()).isEqualTo(101L);
        assertThat(result.getInvitedUserId()).isEqualTo(300L);
        verify(invitationRepository, times(1)).save(any(Invitation.class));
        verify(eventPublisher, times(1)).publishTeamInviteEvent(any(TeamInviteEvent.class));
    }

    @Test
    void sendInvitation_StartupNotFound_ThrowsException() {
        when(startupServiceClient.getStartupById(101L)).thenReturn(null);

        assertThatThrownBy(() -> invitationCommandService.sendInvitation(5L, requestDto))
                .isInstanceOf(StartupNotFoundException.class)
                .hasMessage("Startup not found with id: 101");

        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void sendInvitation_NotFounder_ThrowsException() {
        startupResponseDto.setFounderId(99L);
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);

        assertThatThrownBy(() -> invitationCommandService.sendInvitation(5L, requestDto))
                .isInstanceOf(ForbiddenAccessException.class);

        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    void sendInvitation_SelfInvite_ThrowsException() {
        requestDto.setInvitedUserId(5L);
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);

        assertThatThrownBy(() -> invitationCommandService.sendInvitation(5L, requestDto))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage("You cannot invite yourself to your startup");
    }

    @Test
    void sendInvitation_DuplicateUser_ThrowsException() {
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);
        when(invitationRepository.existsByStartupIdAndInvitedUserIdAndStatus(
                101L, 300L, InvitationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> invitationCommandService.sendInvitation(5L, requestDto))
                .isInstanceOf(DuplicateInvitationException.class)
                .hasMessage("User already has a pending invitation for this startup");
    }

    @Test
    void sendInvitation_DuplicateRole_ThrowsException() {
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);
        when(invitationRepository.existsByStartupIdAndInvitedUserIdAndStatus(
                101L, 300L, InvitationStatus.PENDING)).thenReturn(false);
        when(invitationRepository.existsByStartupIdAndRoleAndStatus(
                101L, TeamRole.CTO, InvitationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> invitationCommandService.sendInvitation(5L, requestDto))
                .isInstanceOf(DuplicateInvitationException.class)
                .hasMessage("This role already has a pending invitation");
    }
}
