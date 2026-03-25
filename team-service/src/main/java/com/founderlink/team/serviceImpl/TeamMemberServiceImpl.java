package com.founderlink.team.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.founderlink.team.command.TeamMemberCommandService;
import com.founderlink.team.dto.request.JoinTeamRequestDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.query.TeamMemberQueryService;
import com.founderlink.team.service.TeamMemberService;

import lombok.RequiredArgsConstructor;

/**
 * Facade that satisfies the existing TeamMemberService contract.
 * Delegates writes → TeamMemberCommandService (CQRS Command side)
 * Delegates reads  → TeamMemberQueryService   (CQRS Query side + Redis cache)
 */
@Service
@RequiredArgsConstructor
public class TeamMemberServiceImpl implements TeamMemberService {

    private final TeamMemberCommandService commandService;
    private final TeamMemberQueryService   queryService;

    @Override
    public TeamMemberResponseDto joinTeam(Long userId, JoinTeamRequestDto requestDto) {
        return commandService.joinTeam(userId, requestDto);
    }

    @Override
    public void removeTeamMember(Long teamMemberId, Long founderId) {
        commandService.removeTeamMember(teamMemberId, founderId);
    }

    @Override
    public List<TeamMemberResponseDto> getTeamByStartupId(Long startupId, Long founderId, String userRole) {
        return queryService.getTeamByStartupId(startupId, founderId, userRole);
    }

    @Override
    public List<TeamMemberResponseDto> getMemberHistory(Long userId) {
        return queryService.getMemberHistory(userId);
    }

    @Override
    public List<TeamMemberResponseDto> getActiveMemberRoles(Long userId) {
        return queryService.getActiveMemberRoles(userId);
    }

    @Override
    public boolean isTeamMember(Long startupId, Long userId) {
        return queryService.isTeamMember(startupId, userId);
    }
}
