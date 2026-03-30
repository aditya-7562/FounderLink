package com.founderlink.team.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberAcceptedEvent {

    private Long invitationId;
    private Long startupId;
    private Long founderId;
    private Long acceptedUserId;
    private String role;
}
