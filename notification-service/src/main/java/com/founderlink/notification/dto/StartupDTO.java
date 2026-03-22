package com.founderlink.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupDTO {
    private Long id;
    private String name;
    private String description;
    private String industry;
    private String solution;
    private Double fundingGoal;
    private String stage;
    private Long founderId;
}
