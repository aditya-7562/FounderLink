package com.founderlink.startup.mapper;

import org.springframework.stereotype.Component;

import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;

@Component
public class StartupMapper {

    // ─────────────────────────────────────────
    // RequestDto + founderId → Entity
    // ─────────────────────────────────────────
    public Startup toEntity(StartupRequestDto requestDto,
                             Long founderId) {

        Startup startup = new Startup();
        startup.setName(requestDto.getName());
        startup.setDescription(
                requestDto.getDescription());
        startup.setIndustry(requestDto.getIndustry());
        startup.setProblemStatement(
                requestDto.getProblemStatement());
        startup.setSolution(requestDto.getSolution());
        startup.setFundingGoal(
                requestDto.getFundingGoal());
        startup.setStage(requestDto.getStage());
        startup.setFounderId(founderId);  // ← from header
        return startup;
    }

    // ─────────────────────────────────────────
    // Entity → ResponseDto
    // ─────────────────────────────────────────
    public StartupResponseDto toResponseDto(
            Startup startup) {

        StartupResponseDto responseDto =
                new StartupResponseDto();
        responseDto.setId(startup.getId());
        responseDto.setName(startup.getName());
        responseDto.setDescription(
                startup.getDescription());
        responseDto.setIndustry(startup.getIndustry());
        responseDto.setProblemStatement(
                startup.getProblemStatement());
        responseDto.setSolution(startup.getSolution());
        responseDto.setFundingGoal(
                startup.getFundingGoal());
        responseDto.setStage(startup.getStage());
        responseDto.setFounderId(
                startup.getFounderId());
        responseDto.setModerationStatus(
                startup.getModerationStatus());
        responseDto.setModerationReason(
                startup.getModerationReason());
        responseDto.setCreatedAt(
                startup.getCreatedAt());
        return responseDto;
    }
}