package com.founderlink.team.dto.response;

import java.time.LocalDateTime;

import com.founderlink.team.entity.TeamRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponseDto {

    private Long id;
    private Long startupId;
    private Long userId;
    private TeamRole role;
    private Boolean isActive;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    
}