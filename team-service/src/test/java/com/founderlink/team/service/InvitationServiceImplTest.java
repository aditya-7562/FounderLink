package com.founderlink.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.founderlink.team.command.InvitationCommandService;
import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.query.InvitationQueryService;
import com.founderlink.team.serviceImpl.InvitationServiceImpl;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTest {

    @Mock
    private InvitationCommandService commandService;

    @Mock
    private InvitationQueryService queryService;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @Test
    void sendInvitation_delegatesToCommandService() {
        InvitationRequestDto requestDto = new InvitationRequestDto();
        InvitationResponseDto responseDto = new InvitationResponseDto();
        when(commandService.sendInvitation(5L, requestDto)).thenReturn(responseDto);

        InvitationResponseDto result = invitationService.sendInvitation(5L, requestDto);

        assertThat(result).isSameAs(responseDto);
        verify(commandService).sendInvitation(5L, requestDto);
    }

    @Test
    void cancelInvitation_delegatesToCommandService() {
        InvitationResponseDto responseDto = new InvitationResponseDto();
        when(commandService.cancelInvitation(1L, 5L)).thenReturn(responseDto);

        InvitationResponseDto result = invitationService.cancelInvitation(1L, 5L);

        assertThat(result).isSameAs(responseDto);
        verify(commandService).cancelInvitation(1L, 5L);
    }

    @Test
    void rejectInvitation_delegatesToCommandService() {
        InvitationResponseDto responseDto = new InvitationResponseDto();
        when(commandService.rejectInvitation(1L, 300L)).thenReturn(responseDto);

        InvitationResponseDto result = invitationService.rejectInvitation(1L, 300L);

        assertThat(result).isSameAs(responseDto);
        verify(commandService).rejectInvitation(1L, 300L);
    }

    @Test
    void getInvitationsByUserId_delegatesToQueryService() {
        List<InvitationResponseDto> list = List.of(new InvitationResponseDto());
        when(queryService.getInvitationsByUserId(300L)).thenReturn(list);

        List<InvitationResponseDto> result = invitationService.getInvitationsByUserId(300L);

        assertThat(result).isSameAs(list);
        verify(queryService).getInvitationsByUserId(300L);
    }

    @Test
    void getInvitationsByUserId_pageable_delegatesToQueryService() {
        Page<InvitationResponseDto> page = new PageImpl<>(List.of());
        Pageable pageable = Pageable.unpaged();
        when(queryService.getInvitationsByUserId(300L, pageable)).thenReturn(page);

        Page<InvitationResponseDto> result = invitationService.getInvitationsByUserId(300L, pageable);

        assertThat(result).isSameAs(page);
        verify(queryService).getInvitationsByUserId(300L, pageable);
    }

    @Test
    void getInvitationsByStartupId_delegatesToQueryService() {
        List<InvitationResponseDto> list = List.of(new InvitationResponseDto());
        when(queryService.getInvitationsByStartupId(101L, 5L)).thenReturn(list);

        List<InvitationResponseDto> result = invitationService.getInvitationsByStartupId(101L, 5L);

        assertThat(result).isSameAs(list);
        verify(queryService).getInvitationsByStartupId(101L, 5L);
    }

    @Test
    void getInvitationsByStartupId_pageable_delegatesToQueryService() {
        Page<InvitationResponseDto> page = new PageImpl<>(List.of());
        Pageable pageable = Pageable.unpaged();
        when(queryService.getInvitationsByStartupId(101L, 5L, pageable)).thenReturn(page);

        Page<InvitationResponseDto> result = invitationService.getInvitationsByStartupId(101L, 5L, pageable);

        assertThat(result).isSameAs(page);
        verify(queryService).getInvitationsByStartupId(101L, 5L, pageable);
    }
}
