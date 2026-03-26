package com.founderlink.team.service.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.command.InvitationCommandService;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.exception.DuplicateInvitationException;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.events.TeamInviteEvent;


@ExtendWith(MockitoExtension.class)
class SendInvitationTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationMapper invitationMapper;

    @Mock
    private TeamEventPublisher eventPublisher;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvitationCommandService invitationService;

    private InvitationRequestDto requestDto;
    private Invitation invitation;
    private InvitationResponseDto responseDto;
    private StartupResponseDto startupResponseDto;   // ← NEW

    @BeforeEach
    void setUp() {
        requestDto = new InvitationRequestDto();
        requestDto.setStartupId(101L);
        requestDto.setInvitedUserId(202L);
        requestDto.setRole(TeamRole.CTO);

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
        responseDto.setStartupId(101L);
        responseDto.setFounderId(5L);
        responseDto.setInvitedUserId(202L);
        responseDto.setRole(TeamRole.CTO);
        responseDto.setStatus(InvitationStatus.PENDING);
        responseDto.setCreatedAt(LocalDateTime.now());

        // Founder owns startup
        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }


    // SUCCESS

    @Test
    void sendInvitation_Success() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);          // ← NEW
        when(invitationRepository
                .existsByStartupIdAndInvitedUserIdAndStatus(
                        101L, 202L, InvitationStatus.PENDING))
                .thenReturn(false);
        when(invitationRepository
                .existsByStartupIdAndRoleAndStatus(
                        101L, TeamRole.CTO,
                        InvitationStatus.PENDING))
                .thenReturn(false);
        when(invitationMapper.toEntity(requestDto, 5L))
                .thenReturn(invitation);
        when(invitationRepository.save(any(Invitation.class)))
                .thenReturn(invitation);
        when(invitationMapper.toResponseDto(invitation))
                .thenReturn(responseDto);

        // Act
        InvitationResponseDto result = invitationService
                .sendInvitation(5L, requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStartupId()).isEqualTo(101L);
        assertThat(result.getFounderId()).isEqualTo(5L);
        assertThat(result.getInvitedUserId()).isEqualTo(202L);
        assertThat(result.getRole()).isEqualTo(TeamRole.CTO);
        assertThat(result.getStatus())
                .isEqualTo(InvitationStatus.PENDING);

        verify(invitationRepository, times(1))
                .save(any(Invitation.class));
        verify(eventPublisher, times(1))
                .publishTeamInviteEvent(
                        any(TeamInviteEvent.class));
    }

    // STARTUP NOT FOUND

    @Test
    void sendInvitation_StartupNotFound_ThrowsException() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(null);                        // ← NEW

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.sendInvitation(5L, requestDto))
                .isInstanceOf(StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 101");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
        verify(eventPublisher, never())
                .publishTeamInviteEvent(any());
    }

    // FOUNDER DOES NOT OWN STARTUP

    @Test
    void sendInvitation_NotOwner_ThrowsException() {

        // Arrange
        // startup founderId is 5
        // but founderId 99 is trying
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.sendInvitation(99L, requestDto))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage(
                        "You are not authorized to " +
                        "perform this action on this startup");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
        verify(eventPublisher, never())
                .publishTeamInviteEvent(any());
    }

    // FOUNDER INVITING THEMSELVES

    @Test
    void sendInvitation_FounderInvitingThemselves_ThrowsException() {

        // Arrange
        requestDto.setInvitedUserId(5L);
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.sendInvitation(5L, requestDto))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessage(
                        "You cannot invite yourself to your startup");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
        verify(eventPublisher, never())
                .publishTeamInviteEvent(any());
    }

    // DUPLICATE INVITATION

    @Test
    void sendInvitation_DuplicateInvitation_ThrowsException() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(invitationRepository
                .existsByStartupIdAndInvitedUserIdAndStatus(
                        101L, 202L, InvitationStatus.PENDING))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.sendInvitation(5L, requestDto))
                .isInstanceOf(DuplicateInvitationException.class)
                .hasMessage(
                        "User already has a pending invitation " +
                        "for this startup");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
        verify(eventPublisher, never())
                .publishTeamInviteEvent(any());
    }

    // DUPLICATE ROLE INVITATION

    @Test
    void sendInvitation_DuplicateRoleInvitation_ThrowsException() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(invitationRepository
                .existsByStartupIdAndInvitedUserIdAndStatus(
                        101L, 202L, InvitationStatus.PENDING))
                .thenReturn(false);
        when(invitationRepository
                .existsByStartupIdAndRoleAndStatus(
                        101L, TeamRole.CTO,
                        InvitationStatus.PENDING))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService.sendInvitation(5L, requestDto))
                .isInstanceOf(DuplicateInvitationException.class)
                .hasMessage(
                        "This role already has a pending invitation");

        verify(invitationRepository, never())
                .save(any(Invitation.class));
        verify(eventPublisher, never())
                .publishTeamInviteEvent(any());
    }
}