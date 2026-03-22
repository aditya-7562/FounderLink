package com.founderlink.team.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupDeletedEvent {

    private Long startupId;
    private Long founderId;
}