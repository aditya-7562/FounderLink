package com.founderlink.investment.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.founderlink.investment.command.InvestmentCommandService;
import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.query.InvestmentQueryService;
import com.founderlink.investment.service.InvestmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Facade that satisfies the existing InvestmentService contract.
 * Delegates writes → InvestmentCommandService (CQRS Command side)
 * Delegates reads  → InvestmentQueryService   (CQRS Query side + Redis cache)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentServiceImpl implements InvestmentService {

    private final InvestmentCommandService commandService;
    private final InvestmentQueryService   queryService;

    @Override
    public InvestmentResponseDto createInvestment(Long investorId, InvestmentRequestDto requestDto) {
        return commandService.createInvestment(investorId, requestDto);
    }

    @Override
    public InvestmentResponseDto updateInvestmentStatus(Long investmentId, Long founderId,
                                                         InvestmentStatusUpdateDto statusUpdateDto) {
        return commandService.updateInvestmentStatus(investmentId, founderId, statusUpdateDto);
    }

    @Override
    public InvestmentResponseDto getInvestmentById(Long investmentId) {
        return queryService.getInvestmentById(investmentId);
    }

    @Override
    public List<InvestmentResponseDto> getInvestmentsByStartupId(Long startupId, Long founderId) {
        return queryService.getInvestmentsByStartupId(startupId, founderId);
    }

    @Override
    public List<InvestmentResponseDto> getInvestmentsByInvestorId(Long investorId) {
        return queryService.getInvestmentsByInvestorId(investorId);
    }
}