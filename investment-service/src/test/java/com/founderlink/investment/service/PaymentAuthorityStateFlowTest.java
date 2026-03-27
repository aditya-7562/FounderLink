package com.founderlink.investment.service;

import com.founderlink.investment.client.StartupServiceClient;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.entity.ManualInvestmentStatus;
import com.founderlink.investment.events.InvestmentEventPublisher;
import com.founderlink.investment.exception.InvalidStatusTransitionException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.repository.InvestmentRepository;
import com.founderlink.investment.serviceImpl.InvestmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAuthorityStateFlowTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private InvestmentEventPublisher eventPublisher;

    @Mock
    private InvestmentMapper investmentMapper;

    @Mock
    private StartupServiceClient startupServiceClient;

    @InjectMocks
    private InvestmentServiceImpl investmentService;

    @Test
    void approvalStaysApprovedUntilPaymentCompletedEvent() {
        Investment investment = new Investment();
        investment.setId(10L);
        investment.setStartupId(20L);
        investment.setInvestorId(30L);
        investment.setAmount(new BigDecimal("500.00"));
        investment.setStatus(InvestmentStatus.PENDING);

        StartupResponseDto startup = new StartupResponseDto();
        startup.setId(20L);
        startup.setFounderId(99L);

        when(investmentRepository.findById(10L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(20L)).thenReturn(startup);
        when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        investmentService.updateInvestmentStatus(
                10L,
                99L,
                new InvestmentStatusUpdateDto(ManualInvestmentStatus.APPROVED));
        assertEquals(InvestmentStatus.APPROVED, investment.getStatus());

        investmentService.markCompletedFromPayment(10L);
        assertEquals(InvestmentStatus.COMPLETED, investment.getStatus());
    }

    @Test
    void manualCompletedIsRejected() {
        Investment investment = new Investment();
        investment.setId(11L);
        investment.setStartupId(22L);
        investment.setStatus(InvestmentStatus.PENDING);

        StartupResponseDto startup = new StartupResponseDto();
        startup.setId(22L);
        startup.setFounderId(77L);

        when(investmentRepository.findById(11L)).thenReturn(Optional.of(investment));
        when(startupServiceClient.getStartupById(22L)).thenReturn(startup);

        assertThrows(
                InvalidStatusTransitionException.class,
                () -> investmentService.updateInvestmentStatus(
                        11L,
                        77L,
                        new InvestmentStatusUpdateDto(ManualInvestmentStatus.COMPLETED)));
    }
}
