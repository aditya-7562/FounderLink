package com.founderlink.investment.query;

import java.util.List;
import java.util.stream.Collectors;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.founderlink.investment.client.StartupServiceClient;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.exception.ForbiddenAccessException;
import com.founderlink.investment.exception.InvestmentNotFoundException;
import com.founderlink.investment.exception.StartupNotFoundException;
import com.founderlink.investment.exception.StartupServiceUnavailableException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.repository.InvestmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentQueryService {

    private final InvestmentRepository investmentRepository;
    private final InvestmentMapper investmentMapper;
    private final StartupServiceClient startupServiceClient;

    // ── getInvestmentById — no Feign call, no retry needed ───────────────────

    @Cacheable(value = "investmentById", key = "#investmentId")
    public InvestmentResponseDto getInvestmentById(Long investmentId) {
        log.info("QUERY - getInvestmentById: {} (cache miss, hitting DB)", investmentId);
        return investmentRepository.findById(investmentId)
                .map(investmentMapper::toResponseDto)
                .orElseThrow(() -> new InvestmentNotFoundException("Investment not found with id: " + investmentId));
    }

    // ── getInvestmentsByStartupId — calls Feign, needs retry + CB ────────────

    @Retry(name = "startupService")
    @CircuitBreaker(name = "startupService", fallbackMethod = "getInvestmentsByStartupIdFallback")
    @Cacheable(value = "investmentsByStartup", key = "#startupId")
    public List<InvestmentResponseDto> getInvestmentsByStartupId(Long startupId, Long founderId) {
        log.info("QUERY - getInvestmentsByStartupId: startupId={}, founderId={} (cache miss, hitting DB)",
                startupId, founderId);
        verifyFounderOwnsStartup(startupId, founderId);
        return investmentRepository.findByStartupId(startupId)
                .stream()
                .map(investmentMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    public List<InvestmentResponseDto> getInvestmentsByStartupIdFallback(Long startupId, Long founderId,
                                                                           Throwable throwable) {
        if (throwable instanceof StartupNotFoundException
                || throwable instanceof ForbiddenAccessException) {
            throw (RuntimeException) throwable;
        }
        log.error("FALLBACK - getInvestmentsByStartupId: circuit open or retries exhausted. Reason: {}", throwable.getMessage());
        throw new StartupServiceUnavailableException(
                "StartupServiceClient#getStartupById",
                "Startup service is temporarily unavailable");
    }

    // ── getInvestmentsByInvestorId — no Feign call, no retry needed ──────────

    @Cacheable(value = "investmentsByInvestor", key = "#investorId")
    public List<InvestmentResponseDto> getInvestmentsByInvestorId(Long investorId) {
        log.info("QUERY - getInvestmentsByInvestorId: {} (cache miss, hitting DB)", investorId);
        return investmentRepository.findByInvestorId(investorId)
                .stream()
                .map(investmentMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // ── Private helper ───────────────────────────────────────────────────────

    private void verifyFounderOwnsStartup(Long startupId, Long founderId) {
        StartupResponseDto startup = startupServiceClient.getStartupById(startupId);
        if (startup == null) {
            throw new StartupNotFoundException("Startup not found with id: " + startupId);
        }
        if (!startup.getFounderId().equals(founderId)) {
            throw new ForbiddenAccessException("You are not authorized to perform this action on this startup");
        }
    }
}
