package com.founderlink.team.service.invitation;

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
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.command.InvitationCommandService;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.events.TeamMemberRejectedEvent;
import com.founderlink.team.exception.InvalidInvitationStatusException;
import com.founderlink.team.exception.InvitationNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class RejectInvitationTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationMapper invitationMapper;

    @Mock
    private TeamEventPublisher teamEventPublisher;

    @Mock
    private com.founderlink.team.client.StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvitationCommandService invitationService;

    private Invitation invitation;
    private InvitationResponseDto responseDto;

    @BeforeEach
    void setUp() {
        invitation = new Invitation();
        invitation.setId(1L);
        invitation.setStartupId(101L);
        invitation.setFounderId(5L);
        invitation.setInvitedUserId(202L);
        invitation.setRole(TeamRole.CTO);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setCreatedAt(LocalDateTime.now());

        responseDto = new InvitationResponseDto();
        responseDto.setId(1L);
        responseDto.setStatus(InvitationStatus.REJECTED);
    }


    // SUCCESS

    @Test
    void rejectInvitation_Success() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class)))
                .thenReturn(invitation);
        when(invitationMapper.toResponseDto(invitation))
                .thenReturn(responseDto);

        // Act
        InvitationResponseDto result = invitationService
                .rejectInvitation(1L, 202L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus())
                .isEqualTo(InvitationStatus.REJECTED);

        verify(invitationRepository, times(1))
                .save(any(Invitation.class));
        verify(teamEventPublisher, times(1))
                .publishTeamMemberRejectedEvent(any(TeamMemberRejectedEvent.class));
    }

    // INVITATION NOT FOUND

    @Test
    void rejectInvitation_NotFound_ThrowsException() {

        // Arrange
        when(invitationRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.rejectInvitation(999L, 202L))
                .isInstanceOf(InvitationNotFoundException.class)
                .hasMessage(
                        "Invitation not found with id: 999");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }

    // WRONG USER
    @Test
    void rejectInvitation_WrongUser_ThrowsException() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.rejectInvitation(1L, 99L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage(
                        "You are not authorized to reject " +
                        "this invitation");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }

    // ALREADY REJECTED

    @Test
    void rejectInvitation_AlreadyRejected_ThrowsException() {

        // Arrange
        invitation.setStatus(InvitationStatus.REJECTED);
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.rejectInvitation(1L, 202L))
                .isInstanceOf(InvalidInvitationStatusException.class)
                .hasMessage(
                        "Only PENDING invitations can be rejected");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }


    // ALREADY ACCEPTED
    
    @Test
    void rejectInvitation_AlreadyAccepted_ThrowsException() {

        // Arrange
        invitation.setStatus(InvitationStatus.ACCEPTED);
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.rejectInvitation(1L, 202L))
                .isInstanceOf(InvalidInvitationStatusException.class)
                .hasMessage(
                        "Only PENDING invitations can be rejected");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }
}
