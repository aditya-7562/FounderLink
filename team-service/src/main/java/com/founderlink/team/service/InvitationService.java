package com.founderlink.team.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;

public interface InvitationService {

    // Founder sends invitation
    InvitationResponseDto sendInvitation(
            Long founderId,
            InvitationRequestDto requestDto);

    // Founder cancels invitation
    InvitationResponseDto cancelInvitation(
            Long invitationId,
            Long founderId);

    // Co-founder rejects invitation
    InvitationResponseDto rejectInvitation(
            Long invitationId,
            Long userId);

    // Co-founder views all their invitations
    List<InvitationResponseDto> getInvitationsByUserId(
            Long userId);

    Page<InvitationResponseDto> getInvitationsByUserId(
            Long userId,
            Pageable pageable);

    // Get all invitations for a startup
    List<InvitationResponseDto> getInvitationsByStartupId(
            Long startupId,Long founderId);

    Page<InvitationResponseDto> getInvitationsByStartupId(
            Long startupId,
            Long founderId,
            Pageable pageable);
}
