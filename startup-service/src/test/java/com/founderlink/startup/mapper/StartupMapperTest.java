package com.founderlink.startup.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;

class StartupMapperTest {

    private StartupMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new StartupMapper();
    }

    @Test
    void toEntity_mapsAllFieldsCorrectly() {
        StartupRequestDto dto = new StartupRequestDto();
        dto.setName("EduReach");
        dto.setDescription("Online education for rural India");
        dto.setIndustry("EdTech");
        dto.setProblemStatement("Rural students lack quality education");
        dto.setSolution("Affordable offline-first learning app");
        dto.setFundingGoal(new BigDecimal("5000000.00"));
        dto.setStage(StartupStage.MVP);

        Startup entity = mapper.toEntity(dto, 42L);

        assertThat(entity.getName()).isEqualTo("EduReach");
        assertThat(entity.getDescription()).isEqualTo("Online education for rural India");
        assertThat(entity.getIndustry()).isEqualTo("EdTech");
        assertThat(entity.getProblemStatement()).isEqualTo("Rural students lack quality education");
        assertThat(entity.getSolution()).isEqualTo("Affordable offline-first learning app");
        assertThat(entity.getFundingGoal()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(entity.getStage()).isEqualTo(StartupStage.MVP);
        assertThat(entity.getFounderId()).isEqualTo(42L);
    }

    @Test
    void toResponseDto_mapsAllFieldsCorrectly() {
        Startup entity = new Startup();
        entity.setId(1L);
        entity.setName("EduReach");
        entity.setDescription("Online education for rural India");
        entity.setIndustry("EdTech");
        entity.setProblemStatement("Rural students lack quality education");
        entity.setSolution("Affordable offline-first learning app");
        entity.setFundingGoal(new BigDecimal("5000000.00"));
        entity.setStage(StartupStage.MVP);
        entity.setFounderId(42L);

        StartupResponseDto dto = mapper.toResponseDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("EduReach");
        assertThat(dto.getDescription()).isEqualTo("Online education for rural India");
        assertThat(dto.getIndustry()).isEqualTo("EdTech");
        assertThat(dto.getProblemStatement()).isEqualTo("Rural students lack quality education");
        assertThat(dto.getSolution()).isEqualTo("Affordable offline-first learning app");
        assertThat(dto.getFundingGoal()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(dto.getStage()).isEqualTo(StartupStage.MVP);
        assertThat(dto.getFounderId()).isEqualTo(42L);
    }

    @Test
    void toResponseDto_preservesCreatedAt() {
        Startup entity = new Startup();
        entity.setId(2L);
        entity.setName("HealthTech");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        entity.setCreatedAt(now);

        StartupResponseDto dto = mapper.toResponseDto(entity);

        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }
}
