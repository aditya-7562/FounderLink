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
<<<<<<< HEAD
    public InvestmentResponseDto createInvestment(Long investorId, InvestmentRequestDto requestDto) {
        return commandService.createInvestment(investorId, requestDto);
=======
    public InvestmentResponseDto createInvestment(Long investorId,
                                                   InvestmentRequestDto requestDto) {

        log.info("Creating investment - investorId: {}, startupId: {}", investorId, requestDto.getStartupId());
        StartupResponseDto startup = startupServiceClient
                .getStartupById(requestDto.getStartupId());

        if (startup == null) {
            throw new StartupNotFoundException(
                    "Startup not found with id: " + requestDto.getStartupId());
        }
    	
    	verifyStartupExists(requestDto.getStartupId());

        // Check duplicate PENDING investment only
        if (investmentRepository
                .existsByStartupIdAndInvestorIdAndStatus(
                        requestDto.getStartupId(),
                        investorId,
                        InvestmentStatus.PENDING)) {
            throw new DuplicateInvestmentException(
                    "You have already invested in this startup");
        }

        Investment investment = investmentMapper
                .toEntity(requestDto, investorId);

        Investment savedInvestment = investmentRepository
                .save(investment);

        InvestmentCreatedEvent event = new InvestmentCreatedEvent(
                savedInvestment.getId(),
                savedInvestment.getStartupId(),
                savedInvestment.getInvestorId(),
                startup.getFounderId(),
                savedInvestment.getAmount()
        );
        eventPublisher.publishInvestmentCreatedEvent(event);

        return investmentMapper.toResponseDto(savedInvestment);
    }


    @Override
    public List<InvestmentResponseDto> getInvestmentsByStartupId(Long startupId,Long founderId) {
    	
    	log.info("Fetching investments for startupId: {}, founderId: {}", startupId, founderId);
    	verifyFounderOwnsStartup(
                startupId, founderId); 

        List<Investment> investments = investmentRepository
                .findByStartupId(startupId);

        return investments.stream()
                .map(investmentMapper::toResponseDto)
                .collect(Collectors.toList());
>>>>>>> 2ccf1aa (fix: payment gateway)
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

        if (statusUpdateDto.getStatus() == com.founderlink.investment.entity.ManualInvestmentStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "COMPLETED can only be set from payment result events");
        }

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
<<<<<<< HEAD

    @Override
    public List<InvestmentResponseDto> getInvestmentsByStartupId(Long startupId, Long founderId) {
        return queryService.getInvestmentsByStartupId(startupId, founderId);
=======

    @Override
    public InvestmentResponseDto markCompletedFromPayment(Long investmentId) {

        log.info("Marking investment as COMPLETED from payment result - investmentId: {}", investmentId);
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new InvestmentNotFoundException(
                        "Investment not found with id: " + investmentId));

        // FIX A: Investment Completion Guard - prevent invalid transitions
        if (investment.getStatus() != InvestmentStatus.APPROVED) {
            return investmentMapper.toResponseDto(investment);
        }

        // FIX A: Duplicate update guard - prevent duplicate marking as COMPLETED
        if (investment.getStatus() == InvestmentStatus.COMPLETED) {
            return investmentMapper.toResponseDto(investment);
        }

        investment.setStatus(InvestmentStatus.COMPLETED);
        return investmentMapper.toResponseDto(investmentRepository.save(investment));
    }

    @Override
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
    
    private void validateStatusTransition(
            InvestmentStatus currentStatus,
            InvestmentStatus newStatus) {

        if (currentStatus == InvestmentStatus.COMPLETED && newStatus == InvestmentStatus.REJECTED) {
            throw new IllegalStateException("Cannot reject completed investment");
        }

        // Cannot update COMPLETED
        if (currentStatus == InvestmentStatus.COMPLETED) {
            throw new InvalidStatusTransitionException(
                    "Cannot update a COMPLETED investment");
        }

        // Cannot update REJECTED
        if (currentStatus == InvestmentStatus.REJECTED) {
            throw new InvalidStatusTransitionException(
                    "Cannot update a REJECTED investment");
        }

        // Cannot update STARTUP_CLOSED          ← ADD
        if (currentStatus ==
                InvestmentStatus.STARTUP_CLOSED) {
            throw new InvalidStatusTransitionException(
                    "Cannot update investment of " +
                    "a closed startup");
        }

        // COMPLETED only after APPROVED
        if (newStatus == InvestmentStatus.COMPLETED
                && currentStatus !=
                InvestmentStatus.APPROVED) {
            throw new InvalidStatusTransitionException(
                    "Investment must be APPROVED " +
                    "before marking COMPLETED");
        }
>>>>>>> 2ccf1aa (fix: payment gateway)
    }

    @Override
    public List<InvestmentResponseDto> getInvestmentsByInvestorId(Long investorId) {
        return queryService.getInvestmentsByInvestorId(investorId);
    }
}