package com.founderlink.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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
import com.founderlink.investment.exception.InvestmentNotFoundException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.query.InvestmentQueryService;
import com.founderlink.investment.repository.InvestmentRepository;

@ExtendWith(MockitoExtension.class)
class GetInvestmentByIdTest {

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
    void getInvestmentById_Success() {

        // Arrange
        when(investmentRepository.findById(1L))
                .thenReturn(Optional.of(investment));
        when(investmentMapper.toResponseDto(investment))
                .thenReturn(responseDto);

        // Act
        InvestmentResponseDto result = investmentService
                .getInvestmentById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStartupId()).isEqualTo(101L);
        assertThat(result.getInvestorId()).isEqualTo(202L);
        assertThat(result.getAmount())
                .isEqualByComparingTo("1000000.00");
        assertThat(result.getStatus())
                .isEqualTo(InvestmentStatus.PENDING);
    }

    // NOT FOUND

    @Test
    void getInvestmentById_NotFound_ThrowsException() {

        // Arrange
        when(investmentRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                investmentService.getInvestmentById(999L))
                .isInstanceOf(InvestmentNotFoundException.class)
                .hasMessage(
                        "Investment not found with id: 999");
    }
}