package com.founderlink.investment.command;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import com.founderlink.investment.client.StartupServiceClient;
import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.events.InvestmentCreatedEvent;
import com.founderlink.investment.events.InvestmentEventPublisher;
import com.founderlink.investment.exception.DuplicateInvestmentException;
import com.founderlink.investment.exception.ForbiddenAccessException;
import com.founderlink.investment.exception.InvalidStatusTransitionException;
import com.founderlink.investment.exception.InvestmentNotFoundException;
import com.founderlink.investment.exception.StartupNotFoundException;
import com.founderlink.investment.exception.StartupServiceUnavailableException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.repository.InvestmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InvestmentCommandService {

    private final InvestmentRepository investmentRepository;
    private final InvestmentEventPublisher eventPublisher;
    private final InvestmentMapper investmentMapper;
    private final StartupServiceClient startupServiceClient;

    // ── createInvestment ─────────────────────────────────────────────────────

    @Retry(name = "startupService")
    @CircuitBreaker(name = "startupService", fallbackMethod = "createInvestmentFallback")
    @Caching(evict = {
        @CacheEvict(value = "investmentsByInvestor", key = "#investorId"),
        @CacheEvict(value = "investmentsByStartup",  key = "#requestDto.startupId")
    })
    public InvestmentResponseDto createInvestment(Long investorId, InvestmentRequestDto requestDto) {
        log.info("COMMAND - createInvestment: investorId={}, startupId={}", investorId, requestDto.getStartupId());

        StartupResponseDto startup = startupServiceClient.getStartupById(requestDto.getStartupId());
        if (startup == null) {
            throw new StartupNotFoundException("Startup not found with id: " + requestDto.getStartupId());
        }

        if (investmentRepository.existsByStartupIdAndInvestorIdAndStatus(
                requestDto.getStartupId(), investorId, InvestmentStatus.PENDING)) {
            throw new DuplicateInvestmentException("You have already invested in this startup");
        }

        Investment investment = investmentMapper.toEntity(requestDto, investorId);
        Investment saved = investmentRepository.save(investment);

        eventPublisher.publishInvestmentCreatedEvent(new InvestmentCreatedEvent(
                saved.getStartupId(), saved.getInvestorId(), startup.getFounderId(), saved.getAmount()));

        return investmentMapper.toResponseDto(saved);
    }

    /**
     * Fallback for createInvestment.
     * Re-throws business exceptions (4xx) — they must not be swallowed.
     * Wraps infrastructure failures (5xx/network) as StartupServiceUnavailableException.
     */
    public InvestmentResponseDto createInvestmentFallback(Long investorId, InvestmentRequestDto requestDto,
                                                           Throwable throwable) {
        if (throwable instanceof StartupNotFoundException
                || throwable instanceof ForbiddenAccessException
                || throwable instanceof DuplicateInvestmentException) {
            throw (RuntimeException) throwable;
        }
        log.error("FALLBACK - createInvestment: circuit open or retries exhausted. Reason: {}", throwable.getMessage());
        throw new StartupServiceUnavailableException(
                "StartupServiceClient#getStartupById",
                "Startup service is temporarily unavailable");
    }

    // ── updateInvestmentStatus ───────────────────────────────────────────────

    @Retry(name = "startupService")
    @CircuitBreaker(name = "startupService", fallbackMethod = "updateInvestmentStatusFallback")
    @Caching(evict = {
        @CacheEvict(value = "investmentById",        key = "#investmentId"),
        @CacheEvict(value = "investmentsByStartup",  allEntries = true),
        @CacheEvict(value = "investmentsByInvestor", allEntries = true)
    })
    public InvestmentResponseDto updateInvestmentStatus(Long investmentId, Long founderId,
                                                         InvestmentStatusUpdateDto statusUpdateDto) {
        log.info("COMMAND - updateInvestmentStatus: investmentId={}, founderId={}, newStatus={}",
                investmentId, founderId, statusUpdateDto.getStatus());

        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new InvestmentNotFoundException("Investment not found with id: " + investmentId));

        verifyFounderOwnsStartup(investment.getStartupId(), founderId);

        InvestmentStatus newStatus = InvestmentStatus.valueOf(statusUpdateDto.getStatus().name());
        validateStatusTransition(investment.getStatus(), newStatus);

        investment.setStatus(newStatus);
        return investmentMapper.toResponseDto(investmentRepository.save(investment));
    }

    public InvestmentResponseDto updateInvestmentStatusFallback(Long investmentId, Long founderId,
                                                                  InvestmentStatusUpdateDto statusUpdateDto,
                                                                  Throwable throwable) {
        if (throwable instanceof InvestmentNotFoundException
                || throwable instanceof ForbiddenAccessException
                || throwable instanceof StartupNotFoundException
                || throwable instanceof InvalidStatusTransitionException) {
            throw (RuntimeException) throwable;
        }
        log.error("FALLBACK - updateInvestmentStatus: circuit open or retries exhausted. Reason: {}", throwable.getMessage());
        throw new StartupServiceUnavailableException(
                "StartupServiceClient#getStartupById",
                "Startup service is temporarily unavailable");
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void verifyFounderOwnsStartup(Long startupId, Long founderId) {
        StartupResponseDto startup = startupServiceClient.getStartupById(startupId);
        if (startup == null) {
            throw new StartupNotFoundException("Startup not found with id: " + startupId);
        }
        if (!startup.getFounderId().equals(founderId)) {
            throw new ForbiddenAccessException("You are not authorized to perform this action on this startup");
        }
    }

    private void validateStatusTransition(InvestmentStatus current, InvestmentStatus next) {
        if (current == InvestmentStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("Cannot update a COMPLETED investment");
        }
        if (current == InvestmentStatus.REJECTED) {
            throw new InvalidStatusTransitionException("Cannot update a REJECTED investment");
        }
        if (current == InvestmentStatus.STARTUP_CLOSED) {
            throw new InvalidStatusTransitionException("Cannot update investment of a closed startup");
        }
        if (next == InvestmentStatus.COMPLETED && current != InvestmentStatus.APPROVED) {
            throw new InvalidStatusTransitionException("Investment must be APPROVED before marking COMPLETED");
        }
    }
}
