package com.founderlink.investment.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.founderlink.investment.command.InvestmentCommandService;
import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.query.InvestmentQueryService;
import com.founderlink.investment.dto.response.StartupResponseDto;
import com.founderlink.investment.entity.Investment;
import com.founderlink.investment.entity.InvestmentStatus;
import com.founderlink.investment.events.InvestmentCreatedEvent;
import com.founderlink.investment.events.InvestmentApprovedEvent;
import com.founderlink.investment.events.InvestmentRejectedEvent;
import com.founderlink.investment.events.InvestmentEventPublisher;
import com.founderlink.investment.exception.DuplicateInvestmentException;
import com.founderlink.investment.exception.ForbiddenAccessException;
import com.founderlink.investment.exception.InvalidStatusTransitionException;
import com.founderlink.investment.exception.InvestmentNotFoundException;
import com.founderlink.investment.exception.StartupNotFoundException;
import com.founderlink.investment.mapper.InvestmentMapper;
import com.founderlink.investment.repository.InvestmentRepository;
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
    public List<InvestmentResponseDto> getInvestmentsByInvestorId(Long investorId) {

        log.info("Fetching investments for investorId: {}", investorId);
        List<Investment> investments = investmentRepository
                .findByInvestorId(investorId);

        return investments.stream()
                .map(investmentMapper::toResponseDto)
                .collect(Collectors.toList());
    }


    @Override
    public InvestmentResponseDto updateInvestmentStatus(
            Long investmentId,
            Long founderId,
            InvestmentStatusUpdateDto statusUpdateDto) {

        log.info("Updating investment status - investmentId: {}, founderId: {}, newStatus: {}", investmentId, founderId, statusUpdateDto.getStatus());
        Investment investment = investmentRepository
                .findById(investmentId)
                .orElseThrow(() ->
                        new InvestmentNotFoundException(
                                "Investment not found with id: "
                                + investmentId));

        // Verify founder owns startup
        verifyFounderOwnsStartup(
                investment.getStartupId(),
                founderId);

        // Convert ManualInvestmentStatus
        // to InvestmentStatus
        InvestmentStatus newStatus =
                InvestmentStatus.valueOf(
                        statusUpdateDto.getStatus().name());

        validateStatusTransition(
                investment.getStatus(),
                newStatus);

        investment.setStatus(newStatus);

        Investment updatedInvestment =
                investmentRepository.save(investment);

        // Publish events for investment status changes
        if (newStatus == InvestmentStatus.APPROVED) {
            InvestmentApprovedEvent approvedEvent = new InvestmentApprovedEvent(
                    updatedInvestment.getId(),
                    updatedInvestment.getInvestorId(),
                    founderId,
                    updatedInvestment.getStartupId(),
                    updatedInvestment.getAmount()
            );
            eventPublisher.publishInvestmentApprovedEvent(approvedEvent);
            log.info("✓ INVESTMENT_APPROVED event published for investmentId: {}",
                    updatedInvestment.getId());
        } else if (newStatus == InvestmentStatus.REJECTED) {
            InvestmentRejectedEvent rejectedEvent = new InvestmentRejectedEvent(
                    updatedInvestment.getId(),
                    updatedInvestment.getInvestorId(),
                    founderId,
                    updatedInvestment.getStartupId(),
                    updatedInvestment.getAmount(),
                    null  // rejectionReason could be added to DTO in future
            );
            eventPublisher.publishInvestmentRejectedEvent(rejectedEvent);
            log.info("✓ INVESTMENT_REJECTED event published for investmentId: {}",
                    updatedInvestment.getId());
        }

        return investmentMapper
                .toResponseDto(updatedInvestment);
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