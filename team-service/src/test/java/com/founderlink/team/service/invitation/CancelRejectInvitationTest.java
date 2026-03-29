package com.founderlink.team.service.invitation;

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
import com.founderlink.team.command.InvitationCommandService;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.exception.InvalidInvitationStatusException;
import com.founderlink.team.exception.InvitationNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class CancelRejectInvitationTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private InvitationMapper invitationMapper;
    @Mock private TeamEventPublisher eventPublisher;
    @Mock private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvitationCommandService invitationCommandService;

    private Invitation invitation;
    private InvitationResponseDto responseDto;

    @BeforeEach
    void setUp() {
        invitation = new Invitation();
        invitation.setId(1L);
        invitation.setStartupId(101L);
        invitation.setFounderId(5L);
        invitation.setInvitedUserId(300L);
        invitation.setRole(TeamRole.CTO);
        invitation.setStatus(InvitationStatus.PENDING);

        responseDto = new InvitationResponseDto();
        responseDto.setId(1L);
        responseDto.setStatus(InvitationStatus.CANCELLED);
    }

    @Test
    void cancelInvitation_Success() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        when(invitationMapper.toResponseDto(invitation)).thenReturn(responseDto);

        InvitationResponseDto result = invitationCommandService.cancelInvitation(1L, 5L);

        assertThat(result).isNotNull();
        verify(invitationRepository, times(1)).save(any(Invitation.class));
    }

    @Test
    void cancelInvitation_NotFound_ThrowsException() {
        when(invitationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationCommandService.cancelInvitation(99L, 5L))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void cancelInvitation_NotFounder_ThrowsException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationCommandService.cancelInvitation(1L, 99L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void cancelInvitation_NotPending_ThrowsException() {
        invitation.setStatus(InvitationStatus.ACCEPTED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationCommandService.cancelInvitation(1L, 5L))
                .isInstanceOf(InvalidInvitationStatusException.class)
                .hasMessage("Only PENDING invitations can be cancelled");
    }

    @Test
    void rejectInvitation_Success() {
        responseDto.setStatus(InvitationStatus.REJECTED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);
        when(invitationMapper.toResponseDto(invitation)).thenReturn(responseDto);

        InvitationResponseDto result = invitationCommandService.rejectInvitation(1L, 300L);

        assertThat(result).isNotNull();
        verify(invitationRepository, times(1)).save(any(Invitation.class));
    }

    @Test
    void rejectInvitation_NotInvitedUser_ThrowsException() {
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationCommandService.rejectInvitation(1L, 99L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void rejectInvitation_NotPending_ThrowsException() {
        invitation.setStatus(InvitationStatus.CANCELLED);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationCommandService.rejectInvitation(1L, 300L))
                .isInstanceOf(InvalidInvitationStatusException.class)
                .hasMessage("Only PENDING invitations can be rejected");
    }
}
