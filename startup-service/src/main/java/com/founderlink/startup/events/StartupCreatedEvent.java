package com.founderlink.startup.events;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupCreatedEvent {

    private Long startupId;
    private String startupName;
    private Long founderId;
    private String industry;
    private BigDecimal fundingGoal;
}