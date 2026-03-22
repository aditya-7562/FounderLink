package com.founderlink.startup.dto.request;

import java.math.BigDecimal;

import com.founderlink.startup.entity.StartupStage;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartupRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Industry is required")
    private String industry;

    @NotBlank(message = "Problem statement is required")
    private String problemStatement;

    @NotBlank(message = "Solution is required")
    private String solution;

    @NotNull(message = "Funding goal is required")
    @DecimalMin(value = "1000.00",
            message = "Minimum funding goal is 1000")
    private BigDecimal fundingGoal;

    @NotNull(message = "Stage is required")
    private StartupStage stage;
}