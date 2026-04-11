package com.founderlink.investment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;

class InvestmentMapperTest {

    private final InvestmentMapper mapper = new InvestmentMapper();

    @Test
    void toEntity() {
        InvestmentRequestDto dto = new InvestmentRequestDto();
        dto.setStartupId(101L);
        dto.setAmount(new BigDecimal("1000.00"));

        Investment entity = mapper.toEntity(dto, 202L);

        assertThat(entity.getStartupId()).isEqualTo(101L);
        assertThat(entity.getInvestorId()).isEqualTo(202L);
        assertThat(entity.getAmount()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    void toResponseDto() {
        Investment entity = new Investment();
        entity.setId(1L);
        entity.setStartupId(101L);
        entity.setInvestorId(202L);
        entity.setAmount(new BigDecimal("1000.00"));
        entity.setStatus(InvestmentStatus.APPROVED);
        entity.setCreatedAt(LocalDateTime.now());

        InvestmentResponseDto dto = mapper.toResponseDto(entity);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getStartupId()).isEqualTo(101L);
        assertThat(dto.getInvestorId()).isEqualTo(202L);
        assertThat(dto.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(dto.getStatus()).isEqualTo(InvestmentStatus.APPROVED);
        assertThat(dto.getCreatedAt()).isEqualTo(entity.getCreatedAt());
    }
}
