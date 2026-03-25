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

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.command.InvitationCommandService;
import com.founderlink.team.exception.InvalidInvitationStatusException;
import com.founderlink.team.exception.InvitationNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class CancelInvitationTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationMapper invitationMapper;

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
        responseDto.setStatus(InvitationStatus.CANCELLED);
    }

    // SUCCESS

    @Test
    void cancelInvitation_Success() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(Invitation.class)))
                .thenReturn(invitation);
        when(invitationMapper.toResponseDto(invitation))
                .thenReturn(responseDto);

        // Act
        InvitationResponseDto result = invitationService
                .cancelInvitation(1L, 5L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus())
                .isEqualTo(InvitationStatus.CANCELLED);

        verify(invitationRepository, times(1))
                .save(any(Invitation.class));
    }

    // INVITATION NOT FOUND

    @Test
    void cancelInvitation_NotFound_ThrowsException() {

        // Arrange
        when(invitationRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.cancelInvitation(999L, 5L))
                .isInstanceOf(InvitationNotFoundException.class)
                .hasMessage(
                        "Invitation not found with id: 999");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }

    // WRONG FOUNDER

    @Test
    void cancelInvitation_WrongFounder_ThrowsException() {

        // Arrange
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.cancelInvitation(1L, 99L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage(
                        "You are not authorized to cancel " +
                        "this invitation");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }


    // ALREADY CANCELLED

    @Test
    void cancelInvitation_AlreadyCancelled_ThrowsException() {

        // Arrange
        invitation.setStatus(InvitationStatus.CANCELLED);
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.cancelInvitation(1L, 5L))
                .isInstanceOf(InvalidInvitationStatusException.class)
                .hasMessage(
                        "Only PENDING invitations can be cancelled");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }
    
    // ALREADY ACCEPTED

    @Test
    void cancelInvitation_AlreadyAccepted_ThrowsException() {

        // Arrange
        invitation.setStatus(InvitationStatus.ACCEPTED);
        when(invitationRepository.findById(1L))
                .thenReturn(Optional.of(invitation));

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.cancelInvitation(1L, 5L))
                .isInstanceOf(InvalidInvitationStatusException.class)
                .hasMessage(
                        "Only PENDING invitations can be cancelled");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
    }
}