package com.founderlink.investment.serviceImpl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.founderlink.investment.command.InvestmentCommandService;
import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.exception.InvestmentNotFoundException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.query.InvestmentQueryService;
import com.founderlink.investment.repository.InvestmentRepository;
import com.founderlink.investment.service.InvestmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentServiceImpl implements InvestmentService {

    private final InvestmentCommandService commandService;
    private final InvestmentQueryService queryService;
    private final InvestmentRepository investmentRepository;
    private final InvestmentMapper investmentMapper;

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

    @Override
    @Caching(evict = {
        @CacheEvict(value = "investmentById", key = "#investmentId"),
        @CacheEvict(value = "investmentsByStartup", allEntries = true),
        @CacheEvict(value = "investmentsByInvestor", allEntries = true)
    })
    public InvestmentResponseDto markCompletedFromPayment(Long investmentId) {
        log.info("Marking investment as COMPLETED from payment result - investmentId: {}", investmentId);
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new InvestmentNotFoundException(
                        "Investment not found with id: " + investmentId));

        if (investment.getStatus() == InvestmentStatus.COMPLETED) {
            return investmentMapper.toResponseDto(investment);
        }

        if (investment.getStatus() != InvestmentStatus.APPROVED) {
            return investmentMapper.toResponseDto(investment);
        }

        investment.setStatus(InvestmentStatus.COMPLETED);
        return investmentMapper.toResponseDto(investmentRepository.save(investment));
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "investmentById", key = "#investmentId"),
        @CacheEvict(value = "investmentsByStartup", allEntries = true),
        @CacheEvict(value = "investmentsByInvestor", allEntries = true)
    })
    public InvestmentResponseDto markPaymentFailedFromPayment(Long investmentId) {
        log.info("Marking investment as PAYMENT_FAILED from payment result - investmentId: {}", investmentId);
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new InvestmentNotFoundException(
                        "Investment not found with id: " + investmentId));

        if (investment.getStatus() == InvestmentStatus.PAYMENT_FAILED) {
            return investmentMapper.toResponseDto(investment);
        }

        if (investment.getStatus() != InvestmentStatus.APPROVED) {
            return investmentMapper.toResponseDto(investment);
        }

        investment.setStatus(InvestmentStatus.PAYMENT_FAILED);
        return investmentMapper.toResponseDto(investmentRepository.save(investment));
    }
}