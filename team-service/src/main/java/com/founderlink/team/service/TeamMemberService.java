package com.founderlink.team.service;

import com.founderlink.team.dto.request.JoinTeamRequestDto;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeamMemberService {

    // JOIN TEAM
   
    TeamMemberResponseDto joinTeam(
            Long userId,
            JoinTeamRequestDto requestDto);
    
    // GET TEAM BY STARTUP ID
    
    List<TeamMemberResponseDto> getTeamByStartupId(
            Long startupId,
            Long founderId,
            String userRole);

    Page<TeamMemberResponseDto> getTeamByStartupId(
            Long startupId,
            Long founderId,
            String userRole,
            Pageable pageable);

    // REMOVE TEAM MEMBER
    // No change

    void removeTeamMember(
            Long teamMemberId,
            Long founderId);

    // IS TEAM MEMBER
    // No change

    boolean isTeamMember(
            Long startupId,
            Long userId);


    // GET MEMBER WORK HISTORY                 ← NEW
    // All records active + inactive
    
    List<TeamMemberResponseDto> getMemberHistory(
            Long userId);

    Page<TeamMemberResponseDto> getMemberHistory(
            Long userId,
            Pageable pageable);

    // GET ACTIVE MEMBER ROLES                 ← NEW
    // Only active roles
    
    List<TeamMemberResponseDto> getActiveMemberRoles(
            Long userId);

    Page<TeamMemberResponseDto> getActiveMemberRoles(
            Long userId,
            Pageable pageable);
}
