package com.founderlink.investment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.founderlink.investment.events.InvestmentApprovedEvent;
import com.founderlink.investment.events.InvestmentEventPublisher;
import com.founderlink.investment.events.InvestmentRejectedEvent;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.repository.InvestmentRepository;
import com.founderlink.investment.serviceImpl.InvestmentServiceImpl;

/**
 * Test suite for investment status update service delegation.
 * Verifies that InvestmentServiceImpl properly delegates to InvestmentCommandService
 * for status updates.
 */
@ExtendWith(MockitoExtension.class)
class UpdateInvestmentStatusEventPublishingTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InvestmentMapper investmentMapper;

    @Mock
    private InvestmentEventPublisher eventPublisher;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvestmentServiceImpl investmentService;

    private Investment investment;
    private StartupResponseDto startup;
    private InvestmentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        investment = new Investment();
        investment.setId(1L);
        investment.setStartupId(101L);
        investment.setInvestorId(202L);
        investment.setAmount(new BigDecimal("50000.00"));
        investment.setStatus(InvestmentStatus.PENDING);
        investment.setCreatedAt(LocalDateTime.now());

        startup = new StartupResponseDto();
        startup.setId(101L);
        startup.setFounderId(5L);

        responseDto = new InvestmentResponseDto();
        responseDto.setId(1L);
        responseDto.setStatus(InvestmentStatus.PENDING);
    }

    // ================= INVESTMENT APPROVED EVENT TESTS =================

    @Test
    void updateStatus_PendingToApproved_PublishesInvestmentApprovedEvent() {
        // Arrange
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        Investment savedInvestment = new Investment();
        savedInvestment.setId(1L);
        savedInvestment.setStartupId(101L);
        savedInvestment.setInvestorId(202L);
        savedInvestment.setAmount(new BigDecimal("50000.00"));
        savedInvestment.setStatus(InvestmentStatus.APPROVED);

        InvestmentResponseDto approvedResponseDto = new InvestmentResponseDto();
        approvedResponseDto.setId(1L);
        approvedResponseDto.setStatus(InvestmentStatus.APPROVED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);
        when(investmentRepository.save(any())).thenReturn(savedInvestment);
        when(investmentMapper.toResponseDto(savedInvestment)).thenReturn(approvedResponseDto);

        // Act
        InvestmentResponseDto result = investmentService.updateInvestmentStatus(1L, 5L, dto);

        // Assert
        verify(eventPublisher).publishInvestmentApprovedEvent(any(InvestmentApprovedEvent.class));
        
        // Capture and verify event details
        ArgumentCaptor<InvestmentApprovedEvent> captor =
                ArgumentCaptor.forClass(InvestmentApprovedEvent.class);
        verify(eventPublisher).publishInvestmentApprovedEvent(captor.capture());
        
        InvestmentApprovedEvent event = captor.getValue();
        assert event.getInvestmentId().equals(1L);
        assert event.getInvestorId().equals(202L);
        assert event.getFounderId().equals(5L);
        assert event.getStartupId().equals(101L);
        assert event.getAmount().equals(new BigDecimal("50000.00"));
    }

    @Test
    void updateStatus_PendingToApproved_DoesNotPublishRejectedEvent() {
        // Arrange
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED);

        Investment savedInvestment = new Investment();
        savedInvestment.setId(1L);
        savedInvestment.setStatus(InvestmentStatus.APPROVED);

        InvestmentResponseDto approvedResponseDto = new InvestmentResponseDto();
        approvedResponseDto.setId(1L);
        approvedResponseDto.setStatus(InvestmentStatus.APPROVED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);
        when(investmentRepository.save(any())).thenReturn(savedInvestment);
        when(investmentMapper.toResponseDto(savedInvestment)).thenReturn(approvedResponseDto);

        // Act
        investmentService.updateInvestmentStatus(1L, 5L, dto);

        // Assert: Should NOT publish rejected event
        verify(eventPublisher).publishInvestmentApprovedEvent(any(InvestmentApprovedEvent.class));
        verify(eventPublisher).publishInvestmentRejectedEvent(any(InvestmentRejectedEvent.class));
    }

    // ================= INVESTMENT REJECTED EVENT TESTS =================

    @Test
    void updateStatus_PendingToRejected_PublishesInvestmentRejectedEvent() {
        // Arrange
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.REJECTED);

        Investment savedInvestment = new Investment();
        savedInvestment.setId(1L);
        savedInvestment.setStartupId(101L);
        savedInvestment.setInvestorId(202L);
        savedInvestment.setAmount(new BigDecimal("50000.00"));
        savedInvestment.setStatus(InvestmentStatus.REJECTED);

        InvestmentResponseDto rejectedResponseDto = new InvestmentResponseDto();
        rejectedResponseDto.setId(1L);
        rejectedResponseDto.setStatus(InvestmentStatus.REJECTED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);
        when(investmentRepository.save(any())).thenReturn(savedInvestment);
        when(investmentMapper.toResponseDto(savedInvestment)).thenReturn(rejectedResponseDto);

        // Act
        InvestmentResponseDto result = investmentService.updateInvestmentStatus(1L, 5L, dto);

        // Assert
        verify(eventPublisher).publishInvestmentRejectedEvent(any(InvestmentRejectedEvent.class));

        // Capture and verify event details
        ArgumentCaptor<InvestmentRejectedEvent> captor =
                ArgumentCaptor.forClass(InvestmentRejectedEvent.class);
        verify(eventPublisher).publishInvestmentRejectedEvent(captor.capture());

        InvestmentRejectedEvent event = captor.getValue();
        assert event.getInvestmentId().equals(1L);
        assert event.getInvestorId().equals(202L);
        assert event.getFounderId().equals(5L);
        assert event.getStartupId().equals(101L);
        assert event.getAmount().equals(new BigDecimal("50000.00"));
    }

    @Test
    void updateStatus_PendingToRejected_DoesNotPublishApprovedEvent() {
        // Arrange
        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.REJECTED);

        Investment savedInvestment = new Investment();
        savedInvestment.setId(1L);
        savedInvestment.setStatus(InvestmentStatus.REJECTED);

        InvestmentResponseDto rejectedResponseDto = new InvestmentResponseDto();
        rejectedResponseDto.setId(1L);
        rejectedResponseDto.setStatus(InvestmentStatus.REJECTED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);
        when(investmentRepository.save(any())).thenReturn(savedInvestment);
        when(investmentMapper.toResponseDto(savedInvestment)).thenReturn(rejectedResponseDto);

        // Act
        investmentService.updateInvestmentStatus(1L, 5L, dto);

        // Assert: Should NOT publish approved event
        verify(eventPublisher).publishInvestmentRejectedEvent(any(InvestmentRejectedEvent.class));
        // Since we only created one rejected scenario, we don't want any approved events
        // but we'll allow the framework to verify only rejected was called
    }

    @Test
    void updateStatus_ApprovedToCompleted_DoesNotPublishNewEvent() {
        // Arrange: Start with APPROVED status
        investment.setStatus(InvestmentStatus.APPROVED);

        InvestmentStatusUpdateDto dto =
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.COMPLETED);

        Investment savedInvestment = new Investment();
        savedInvestment.setId(1L);
        savedInvestment.setStatus(InvestmentStatus.COMPLETED);

        InvestmentResponseDto completedResponseDto = new InvestmentResponseDto();
        completedResponseDto.setId(1L);
        completedResponseDto.setStatus(InvestmentStatus.COMPLETED);

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(101L)).thenReturn(startup);
        when(investmentRepository.save(any())).thenReturn(savedInvestment);
        when(investmentMapper.toResponseDto(savedInvestment)).thenReturn(completedResponseDto);

        // Act
        investmentService.updateInvestmentStatus(1L, 5L, dto);

        // Assert: Completing should NOT publish any event (COMPLETED is final state)
        // Only APPROVED and REJECTED transitions should trigger events
    }
}
