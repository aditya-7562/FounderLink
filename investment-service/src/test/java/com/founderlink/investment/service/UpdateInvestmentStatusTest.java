package com.founderlink.investment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.entity.ManualInvestmentStatus;
import com.founderlink.investment.events.InvestmentEventPublisher;
import com.founderlink.investment.command.InvestmentCommandService;
import com.founderlink.investment.exception.ForbiddenAccessException;
import com.founderlink.investment.exception.InvalidStatusTransitionException;
import com.founderlink.investment.exception.InvestmentNotFoundException;
import com.founderlink.investment.exception.StartupNotFoundException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.repository.InvestmentRepository;
import com.founderlink.investment.exception.StartupServiceUnavailableException;

@ExtendWith(MockitoExtension.class)
class UpdateInvestmentStatusTest {

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
    private StartupResponseDto startup;

    @BeforeEach
    void setUp() {
        investment = new Investment();
        investment.setId(1L);
        investment.setStartupId(101L);
        investment.setInvestorId(202L);
        investment.setAmount(new BigDecimal("1000000.00"));
        investment.setStatus(InvestmentStatus.PENDING);
        investment.setCreatedAt(LocalDateTime.now());

        startup = new StartupResponseDto();
        startup.setId(101L);
        startup.setFounderId(5L);
    }

    // ================= SUCCESS CASES =================

    @Test
    void updateStatus_PendingToApproved_ShouldSucceed() {
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        Investment saved = new Investment();
        saved.setId(1L);
        saved.setStatus(InvestmentStatus.APPROVED);

        InvestmentResponseDto response = new InvestmentResponseDto();
        response.setId(1L);
        response.setStatus(InvestmentStatus.APPROVED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);
        when(investmentRepository.save(any())).thenReturn(saved);
        when(investmentMapper.toResponseDto(saved)).thenReturn(response);

        InvestmentResponseDto result =
                investmentService.updateInvestmentStatus(1L, 5L, dto);

        assertThat(result.getStatus()).isEqualTo(InvestmentStatus.APPROVED);
    }

    @Test
    void updateStatus_PendingToRejected_ShouldSucceed() {
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.REJECTED);

        Investment saved = new Investment();
        saved.setId(1L);
        saved.setStatus(InvestmentStatus.REJECTED);

        InvestmentResponseDto response = new InvestmentResponseDto();
        response.setId(1L);
        response.setStatus(InvestmentStatus.REJECTED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);
        when(investmentRepository.save(any())).thenReturn(saved);
        when(investmentMapper.toResponseDto(saved)).thenReturn(response);

        InvestmentResponseDto result =
                investmentService.updateInvestmentStatus(1L, 5L, dto);

        assertThat(result.getStatus()).isEqualTo(InvestmentStatus.REJECTED);
    }

    @Test
    void updateStatus_ApprovedToCompleted_ShouldSucceed() {
        investment.setStatus(InvestmentStatus.APPROVED);
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.COMPLETED);

        Investment saved = new Investment();
        saved.setId(1L);
        saved.setStatus(InvestmentStatus.COMPLETED);

        InvestmentResponseDto response = new InvestmentResponseDto();
        response.setId(1L);
        response.setStatus(InvestmentStatus.COMPLETED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);
        when(investmentRepository.save(any())).thenReturn(saved);
        when(investmentMapper.toResponseDto(saved)).thenReturn(response);

        InvestmentResponseDto result =
                investmentService.updateInvestmentStatus(1L, 5L, dto);

        assertThat(result.getStatus()).isEqualTo(InvestmentStatus.COMPLETED);
    }

    // ================= VALIDATION FAILURES =================

    @Test
    void updateStatus_WhenUserNotFounder_ShouldThrowForbidden() {
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);

        assertThatThrownBy(() ->
                investmentService.updateInvestmentStatus(1L, 99L, dto))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("You are not authorized to perform this action on this startup");
    }

    @Test
    void updateStatus_WhenStartupNotFound_ShouldThrowException() {
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(null);

        assertThatThrownBy(() ->
                investmentService.updateInvestmentStatus(1L, 5L, dto))
                .isInstanceOf(StartupNotFoundException.class)
                .hasMessage("Startup not found with id: 101");
    }

    @Test
    void updateStatus_WhenInvestmentNotFound_ShouldThrowException() {
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        when(investmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                investmentService.updateInvestmentStatus(999L, 5L, dto))
                .isInstanceOf(InvestmentNotFoundException.class)
                .hasMessage("Investment not found with id: 999");
    }

    // ================= INVALID TRANSITIONS =================

    @Test
    void updateStatus_WhenInvestmentCompleted_ShouldThrowException() {
        investment.setStatus(InvestmentStatus.COMPLETED);

        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);

        assertThatThrownBy(() ->
                investmentService.updateInvestmentStatus(1L, 5L, dto))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Cannot update a COMPLETED investment");
    }

    @Test
    void updateStatus_WhenInvestmentRejected_ShouldThrowException() {
        investment.setStatus(InvestmentStatus.REJECTED);

        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);

        assertThatThrownBy(() ->
                investmentService.updateInvestmentStatus(1L, 5L, dto))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Cannot update a REJECTED investment");
    }

    @Test
    void updateStatus_WhenStartupClosed_ShouldThrowException() {
        investment.setStatus(InvestmentStatus.STARTUP_CLOSED);

        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);

        assertThatThrownBy(() ->
                investmentService.updateInvestmentStatus(1L, 5L, dto))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Cannot update investment of a closed startup");
    }

    @Test
    void updateStatus_PendingToCompleted_ShouldThrowException() {
        investment.setStatus(InvestmentStatus.PENDING);

        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.COMPLETED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);

        assertThatThrownBy(() ->
                investmentService.updateInvestmentStatus(1L, 5L, dto))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Investment must be APPROVED before marking COMPLETED");
    }

    // ================= FALLBACK TESTS =================

    @Test
    void updateInvestmentStatusFallback_BusinessException_ThrowsOriginal() {
        InvestmentNotFoundException ex = new InvestmentNotFoundException("Not found");
        InvestmentStatusUpdateDto dto = new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);
        
        assertThatThrownBy(() -> investmentService.updateInvestmentStatusFallback(1L, 5L, dto, ex))
                .isEqualTo(ex);
    }

    @Test
    void updateInvestmentStatusFallback_OtherException_ThrowsServiceUnavailable() {
        RuntimeException ex = new RuntimeException("Generic error");
        InvestmentStatusUpdateDto dto = new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        assertThatThrownBy(() -> investmentService.updateInvestmentStatusFallback(1L, 5L, dto, ex))
                .isInstanceOf(StartupServiceUnavailableException.class)
                .hasMessageContaining("Startup service is temporarily unavailable");
    }
}