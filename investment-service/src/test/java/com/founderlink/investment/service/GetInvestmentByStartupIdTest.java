package com.founderlink.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.exception.ForbiddenAccessException;
import com.founderlink.investment.exception.StartupNotFoundException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.query.InvestmentQueryService;
import com.founderlink.investment.repository.InvestmentRepository;

@ExtendWith(MockitoExtension.class)
class GetInvestmentByStartupIdTest {

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
    private StartupResponseDto startupResponseDto;

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

        // Founder owns startup
        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }

    // SUCCESS
    
    @Test
    void getInvestmentsByStartupId_Success() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(investmentRepository.findByStartupId(101L))
                .thenReturn(List.of(investment));
        when(investmentMapper.toResponseDto(investment))
                .thenReturn(responseDto);

        // Act
        List<InvestmentResponseDto> result = investmentService
                .getInvestmentsByStartupId(101L, 5L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStartupId())
                .isEqualTo(101L);
    }


    // STARTUP NOT FOUND

    @Test
    void getInvestmentsByStartupId_StartupNotFound_ThrowsException() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() ->
                investmentService
                        .getInvestmentsByStartupId(101L, 5L))
                .isInstanceOf(StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 101");
    }

    // FOUNDER DOES NOT OWN STARTUP
    
    @Test
    void getInvestmentsByStartupId_NotOwner_ThrowsException() {

        // Arrange
        // startup founderId is 5
        // but founderId 99 is trying to access
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);

        // Act & Assert
        assertThatThrownBy(() ->
                investmentService
                        .getInvestmentsByStartupId(101L, 99L))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage(
                        "You are not authorized to " +
                        "perform this action on this startup");
    }

    // EMPTY LIST

    @Test
    void getInvestmentsByStartupId_NoInvestments_ReturnsEmptyList() {

        // Arrange
        when(startupServiceClient.getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(investmentRepository.findByStartupId(101L))
                .thenReturn(List.of());

        // Act
        List<InvestmentResponseDto> result = investmentService
                .getInvestmentsByStartupId(101L, 5L);

        // Assert
        assertThat(result).isEmpty();
    }
}