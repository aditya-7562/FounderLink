package com.founderlink.team.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberRejectedEvent {

    private Long invitationId;
    private Long startupId;
    private Long founderId;
    private Long rejectedUserId;
    private String role;
}
