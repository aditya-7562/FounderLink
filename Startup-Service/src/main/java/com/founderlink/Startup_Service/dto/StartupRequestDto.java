package com.founderlink.Startup_Service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.aspectj.bridge.IMessage;

@Data
public class StartupRequestDto {


    private Long id;

    @NotBlank(message = "Name is Required")
    private String name;

    @NotBlank(message = "Description is Required")
    private String description;

    @NotBlank(message = "Industry is Required")
    private String industry;
    @NotBlank(message = "Problem Statement is Required")
    private String problemStatement;
    @NotBlank(message = "Solution is Required")
    private String solution;
    @NotNull(message = "Funding Goal is required")
    private Double fundingGoal;
    private String stage;

    @NotNull(message = "Founder Id is required")
    private Long founderId;
}
