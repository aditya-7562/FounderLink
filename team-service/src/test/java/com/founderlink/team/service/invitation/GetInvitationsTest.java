package com.founderlink.team.service.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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

    @Mock private InvitationRepository invitationRepository;
    @Mock private InvitationMapper invitationMapper;
    @Mock private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvitationQueryService invitationQueryService;

    private Invitation invitation;
    private InvitationResponseDto responseDto;
    private StartupResponseDto startupResponseDto;

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
        responseDto.setStartupId(101L);
        responseDto.setInvitedUserId(300L);
        responseDto.setStatus(InvitationStatus.PENDING);

        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }

    @Test
    void getInvitationsByUserId_Success() {
        when(invitationRepository.findByInvitedUserId(300L)).thenReturn(List.of(invitation));
        when(invitationMapper.toResponseDto(invitation)).thenReturn(responseDto);

        List<InvitationResponseDto> result = invitationQueryService.getInvitationsByUserId(300L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvitedUserId()).isEqualTo(300L);
    }

    @Test
    void getInvitationsByUserId_Empty() {
        when(invitationRepository.findByInvitedUserId(300L)).thenReturn(List.of());

        List<InvitationResponseDto> result = invitationQueryService.getInvitationsByUserId(300L);

        assertThat(result).isEmpty();
    }

    @Test
    void getInvitationsByStartupId_Success() {
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);
        when(invitationRepository.findByStartupId(101L)).thenReturn(List.of(invitation));
        when(invitationMapper.toResponseDto(invitation)).thenReturn(responseDto);

        List<InvitationResponseDto> result = invitationQueryService.getInvitationsByStartupId(101L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartupId()).isEqualTo(101L);
    }

    @Test
    void getInvitationsByStartupId_StartupNotFound_ThrowsException() {
        when(startupServiceClient.getStartupById(101L)).thenReturn(null);

        assertThatThrownBy(() -> invitationQueryService.getInvitationsByStartupId(101L, 5L))
                .isInstanceOf(StartupNotFoundException.class);
    }

    @Test
    void getInvitationsByStartupId_NotFounder_ThrowsException() {
        startupResponseDto.setFounderId(99L);
        when(startupServiceClient.getStartupById(101L)).thenReturn(startupResponseDto);

        assertThatThrownBy(() -> invitationQueryService.getInvitationsByStartupId(101L, 5L))
                .isInstanceOf(ForbiddenAccessException.class);
    }
}
