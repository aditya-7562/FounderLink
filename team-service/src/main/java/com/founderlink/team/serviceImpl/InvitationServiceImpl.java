package com.founderlink.team.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.founderlink.team.command.InvitationCommandService;
import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.query.InvitationQueryService;
import com.founderlink.team.service.InvitationService;

import lombok.RequiredArgsConstructor;

/**
 * Facade that satisfies the existing InvitationService contract.
 * Delegates writes → InvitationCommandService (CQRS Command side)
 * Delegates reads  → InvitationQueryService   (CQRS Query side + Redis cache)
 */
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final InvitationCommandService commandService;
    private final InvitationQueryService   queryService;

    @Override
    public InvitationResponseDto sendInvitation(Long founderId, InvitationRequestDto requestDto) {
        return commandService.sendInvitation(founderId, requestDto);
    }

    @Override
    public InvitationResponseDto cancelInvitation(Long invitationId, Long founderId) {
        return commandService.cancelInvitation(invitationId, founderId);
    }

    @Override
    public InvitationResponseDto rejectInvitation(Long invitationId, Long userId) {
        return commandService.rejectInvitation(invitationId, userId);
    }

    @Override
    public List<InvitationResponseDto> getInvitationsByUserId(Long userId) {
        return queryService.getInvitationsByUserId(userId);
    }

    @Override
    public List<InvitationResponseDto> getInvitationsByStartupId(Long startupId, Long founderId) {
        return queryService.getInvitationsByStartupId(startupId, founderId);
    }
}
