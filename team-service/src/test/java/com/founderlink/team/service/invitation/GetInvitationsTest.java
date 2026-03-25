package com.founderlink.team.service.invitation;

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
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.entity.TeamRole;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.query.InvitationQueryService;
import com.founderlink.team.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class GetInvitationsTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private InvitationMapper invitationMapper;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvitationQueryService invitationService;

    private Invitation invitation;
    private InvitationResponseDto responseDto;
    private StartupResponseDto startupResponseDto;

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
        responseDto.setStartupId(101L);
        responseDto.setFounderId(5L);
        responseDto.setInvitedUserId(202L);
        responseDto.setRole(TeamRole.CTO);
        responseDto.setStatus(InvitationStatus.PENDING);
        responseDto.setCreatedAt(LocalDateTime.now());

        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }

    // GET BY USER ID — SUCCESS

    @Test
    void getInvitationsByUserId_Success() {

        // Arrange
        // No FeignClient needed
        // user sees their own invitations
        when(invitationRepository
                .findByInvitedUserId(202L))
                .thenReturn(List.of(invitation));
        when(invitationMapper.toResponseDto(invitation))
                .thenReturn(responseDto);

        // Act
        List<InvitationResponseDto> result = invitationService
                .getInvitationsByUserId(202L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvitedUserId())
                .isEqualTo(202L);
    }
    
    // GET BY USER ID — EMPTY

    @Test
    void getInvitationsByUserId_NoInvitations_ReturnsEmptyList() {

        // Arrange
        when(invitationRepository
                .findByInvitedUserId(202L))
                .thenReturn(List.of());

        // Act
        List<InvitationResponseDto> result = invitationService
                .getInvitationsByUserId(202L);

        // Assert
        assertThat(result).isEmpty();
    }

    // GET BY STARTUP ID — SUCCESS

    @Test
    void getInvitationsByStartupId_Success() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(invitationRepository.findByStartupId(101L))
                .thenReturn(List.of(invitation));
        when(invitationMapper.toResponseDto(invitation))
                .thenReturn(responseDto);

        // Act
        List<InvitationResponseDto> result = invitationService
                .getInvitationsByStartupId(101L, 5L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartupId())
                .isEqualTo(101L);
    }
    
    // GET BY STARTUP ID — NOT OWNER

    @Test
    void getInvitationsByStartupId_NotOwner_ThrowsException() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService
                        .getInvitationsByStartupId(101L, 99L))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage(
                        "You are not authorized to " +
                        "perform this action on this startup");
    }

    // GET BY STARTUP ID — STARTUP NOT FOUND

    @Test
    void getInvitationsByStartupId_StartupNotFound_ThrowsException() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() ->
                invitationService
                        .getInvitationsByStartupId(101L, 5L))
                .isInstanceOf(StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 101");
    }

    // GET BY STARTUP ID — EMPTY
    
    @Test
    void getInvitationsByStartupId_NoInvitations_ReturnsEmptyList() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(invitationRepository.findByStartupId(101L))
                .thenReturn(List.of());

        // Act
        List<InvitationResponseDto> result = invitationService
                .getInvitationsByStartupId(101L, 5L);

        // Assert
        assertThat(result).isEmpty();
    }
}