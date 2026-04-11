package com.founderlink.startup.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.founderlink.startup.entity.StartupStage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupResponseDto {

    private Long id;
    private String name;
    private String description;
    private String industry;
    private String problemStatement;
    private String solution;
    private BigDecimal fundingGoal;
    private StartupStage stage;
    private Long founderId;          // ← critical for FeignClient
    private String moderationStatus;
    private String moderationReason;
    private LocalDateTime createdAt;
}