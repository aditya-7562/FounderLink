package com.founderlink.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.investment.client.StartupServiceClient;
import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.events.InvestmentCreatedEvent;
import com.founderlink.investment.command.InvestmentCommandService;
import com.founderlink.investment.events.InvestmentEventPublisher;
import com.founderlink.investment.exception.DuplicateInvestmentException;
import com.founderlink.investment.exception.StartupNotFoundException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.repository.InvestmentRepository;

@ExtendWith(MockitoExtension.class)
class CreateInvestmentTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InvestmentMapper investmentMapper;

    @Mock
    private InvestmentEventPublisher eventPublisher;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvestmentCommandService investmentService;

    private Investment investment;
    private InvestmentRequestDto requestDto;
    private InvestmentResponseDto responseDto;
    private StartupResponseDto startupResponseDto;

    @BeforeEach
    void setUp() {
        investment = new Investment();
        investment.setId(1L);
        investment.setStartupId(101L);
        investment.setInvestorId(202L);
        investment.setAmount(
                new BigDecimal("1000000.00"));
        investment.setStatus(InvestmentStatus.PENDING);
        investment.setCreatedAt(LocalDateTime.now());

        requestDto = new InvestmentRequestDto();
        requestDto.setStartupId(101L);
        requestDto.setAmount(
                new BigDecimal("1000000.00"));

        responseDto = new InvestmentResponseDto();
        responseDto.setId(1L);
        responseDto.setStartupId(101L);
        responseDto.setInvestorId(202L);
        responseDto.setAmount(
                new BigDecimal("1000000.00"));
        responseDto.setStatus(InvestmentStatus.PENDING);
        responseDto.setCreatedAt(LocalDateTime.now());

        startupResponseDto = new StartupResponseDto();
        startupResponseDto.setId(101L);
        startupResponseDto.setFounderId(5L);
    }


    // SUCCESS

    @Test
    void createInvestment_Success() {

        // Arrange
        when(startupServiceClient
                .getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(investmentRepository
                .existsByStartupIdAndInvestorIdAndStatus(
                        101L, 202L,
                        InvestmentStatus.PENDING))
                .thenReturn(false);
        when(investmentMapper
                .toEntity(requestDto, 202L))
                .thenReturn(investment);
        when(investmentRepository
                .save(any(Investment.class)))
                .thenReturn(investment);
        when(investmentMapper
                .toResponseDto(investment))
                .thenReturn(responseDto);

        // Act
        InvestmentResponseDto result = investmentService
                .createInvestment(202L, requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStartupId())
                .isEqualTo(101L);
        assertThat(result.getInvestorId())
                .isEqualTo(202L);
        assertThat(result.getAmount())
                .isEqualByComparingTo("1000000.00");
        assertThat(result.getStatus())
                .isEqualTo(InvestmentStatus.PENDING);

        // Verify
        verify(startupServiceClient, times(1))
                .getStartupById(101L);
        verify(investmentRepository, times(1))
                .save(any(Investment.class));
        verify(eventPublisher, times(1))
                .publishInvestmentCreatedEvent(
                        any(InvestmentCreatedEvent.class));
    }

    // STARTUP NOT FOUND

    @Test
    void createInvestment_StartupNotFound_ThrowsException() {

        // Arrange
        when(startupServiceClient
                .getStartupById(101L))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() ->
                investmentService
                        .createInvestment(
                                202L, requestDto))
                .isInstanceOf(
                        StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 101");

        verify(investmentRepository, never())
                .save(any(Investment.class));
        verify(eventPublisher, never())
                .publishInvestmentCreatedEvent(any());
    }


    // DUPLICATE INVESTMENT

    @Test
    void createInvestment_DuplicateInvestment_ThrowsException() {

        // Arrange
        when(startupServiceClient
                .getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(investmentRepository
                .existsByStartupIdAndInvestorIdAndStatus(
                        101L, 202L,
                        InvestmentStatus.PENDING))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                investmentService
                        .createInvestment(
                                202L, requestDto))
                .isInstanceOf(
                        DuplicateInvestmentException.class)
                .hasMessage(
                        "You have already invested " +
                        "in this startup");

        verify(investmentRepository, never())
                .save(any(Investment.class));
        verify(eventPublisher, never())
                .publishInvestmentCreatedEvent(any());
    }

    // REJECTED ALLOWS NEW INVESTMENT

    @Test
    void createInvestment_AfterRejection_AllowsNew() {

        // Arrange
        when(startupServiceClient
                .getStartupById(101L))
                .thenReturn(startupResponseDto);
        when(investmentRepository
                .existsByStartupIdAndInvestorIdAndStatus(
                        101L, 202L,
                        InvestmentStatus.PENDING))
                .thenReturn(false);
        when(investmentMapper
                .toEntity(requestDto, 202L))
                .thenReturn(investment);
        when(investmentRepository
                .save(any(Investment.class)))
                .thenReturn(investment);
        when(investmentMapper
                .toResponseDto(investment))
                .thenReturn(responseDto);

        // Act
        InvestmentResponseDto result =
                investmentService
                        .createInvestment(
                                202L, requestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus())
                .isEqualTo(InvestmentStatus.PENDING);

        verify(investmentRepository, times(1))
                .save(any(Investment.class));
    }
}