package com.founderlink.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.investment.client.StartupServiceClient;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.query.InvestmentQueryService;
import com.founderlink.investment.repository.InvestmentRepository;

@ExtendWith(MockitoExtension.class)
class GetInvestmentByInvestorIdTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InvestmentMapper investmentMapper;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvestmentQueryService investmentService;

    private Investment investment;
    private InvestmentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        investment = new Investment();
        investment.setId(1L);
        investment.setStartupId(101L);
        investment.setInvestorId(202L);
        investment.setAmount(new BigDecimal("1000000.00"));
        investment.setStatus(InvestmentStatus.PENDING);
        investment.setCreatedAt(LocalDateTime.now());

        responseDto = new InvestmentResponseDto();
        responseDto.setId(1L);
        responseDto.setStartupId(101L);
        responseDto.setInvestorId(202L);
        responseDto.setAmount(new BigDecimal("1000000.00"));
        responseDto.setStatus(InvestmentStatus.PENDING);
        responseDto.setCreatedAt(LocalDateTime.now());
    }

    // SUCCESS
    
    @Test
    void getInvestmentsByInvestorId_Success() {

        // Arrange
        // No FeignClient needed
        // investor sees their own data
        when(investmentRepository.findByInvestorId(202L))
                .thenReturn(List.of(investment));
        when(investmentMapper.toResponseDto(investment))
                .thenReturn(responseDto);

        // Act
        List<InvestmentResponseDto> result = investmentService
                .getInvestmentsByInvestorId(202L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvestorId())
                .isEqualTo(202L);
    }

    // EMPTY LIST

    @Test
    void getInvestmentsByInvestorId_NoInvestments_ReturnsEmptyList() {

        // Arrange
        when(investmentRepository.findByInvestorId(202L))
                .thenReturn(List.of());

        // Act
        List<InvestmentResponseDto> result = investmentService
                .getInvestmentsByInvestorId(202L);

        // Assert
        assertThat(result).isEmpty();
    }


    // MULTIPLE STARTUPS

    @Test
    void getInvestmentsByInvestorId_MultipleStartups_ReturnsAll() {

        // Arrange
        Investment investment2 = new Investment();
        investment2.setId(2L);
        investment2.setStartupId(202L);
        investment2.setInvestorId(202L);
        investment2.setAmount(new BigDecimal("500000.00"));
        investment2.setStatus(InvestmentStatus.APPROVED);

        InvestmentResponseDto responseDto2 =
                new InvestmentResponseDto();
        responseDto2.setId(2L);
        responseDto2.setStartupId(202L);
        responseDto2.setInvestorId(202L);
        responseDto2.setAmount(new BigDecimal("500000.00"));
        responseDto2.setStatus(InvestmentStatus.APPROVED);

        when(investmentRepository.findByInvestorId(202L))
                .thenReturn(List.of(investment, investment2));
        when(investmentMapper.toResponseDto(investment))
                .thenReturn(responseDto);
        when(investmentMapper.toResponseDto(investment2))
                .thenReturn(responseDto2);

        // Act
        List<InvestmentResponseDto> result = investmentService
                .getInvestmentsByInvestorId(202L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStartupId())
                .isEqualTo(101L);
        assertThat(result.get(1).getStartupId())
                .isEqualTo(202L);
    }
}