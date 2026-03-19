package com.founderlink.Startup_Service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupCreatedEvent {
    private Long startupId;
    private Long founderId;
    private String industry;
    private Double fundingGoal;
}
