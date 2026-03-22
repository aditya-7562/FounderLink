package com.founderlink.team.serviceImpl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.founderlink.team.client.StartupServiceClient;
import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.dto.response.StartupResponseDto;
import com.founderlink.team.entity.Invitation;
import com.founderlink.team.entity.InvitationStatus;
import com.founderlink.team.events.TeamEventPublisher;
import com.founderlink.team.events.TeamInviteEvent;
import com.founderlink.team.exception.DuplicateInvitationException;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.exception.InvalidInvitationStatusException;
import com.founderlink.team.exception.InvitationNotFoundException;
import com.founderlink.team.exception.StartupNotFoundException;
import com.founderlink.team.exception.UnauthorizedAccessException;
import com.founderlink.team.mapper.InvitationMapper;
import com.founderlink.team.repository.InvitationRepository;
import com.founderlink.team.service.InvitationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;
    private final TeamEventPublisher eventPublisher;
    private final StartupServiceClient startupServiceClient;

    // SEND INVITATION

    @Override
    public InvitationResponseDto sendInvitation(Long founderId,InvitationRequestDto requestDto) {
    	
    	verifyFounderOwnsStartup(requestDto.getStartupId(),founderId);

        // Edge case 1 — founder inviting themselves
        if (founderId.equals(requestDto.getInvitedUserId())) {
            throw new UnauthorizedAccessException(
                    "You cannot invite yourself to your startup");
        }

        // Edge case 2 — duplicate invitation
        if (invitationRepository
                .existsByStartupIdAndInvitedUserIdAndStatus(
                        requestDto.getStartupId(),
                        requestDto.getInvitedUserId(),
                        InvitationStatus.PENDING)) {
            throw new DuplicateInvitationException(
                    "User already has a pending invitation for this startup");
        }

        // Edge case 3 — duplicate role invitation
        if (invitationRepository
                .existsByStartupIdAndRoleAndStatus(
                        requestDto.getStartupId(),
                        requestDto.getRole(),
                        InvitationStatus.PENDING)) {
            throw new DuplicateInvitationException(
                    "This role already has a pending invitation");
        }

        // Map DTO to Entity
        Invitation invitation = invitationMapper
                .toEntity(requestDto, founderId);

        // Save to DB
        Invitation savedInvitation = invitationRepository
                .save(invitation);

        // Publish RabbitMQ Event
        TeamInviteEvent event = new TeamInviteEvent(
                savedInvitation.getStartupId(),
                savedInvitation.getInvitedUserId(),
                savedInvitation.getRole().name()
        );
        eventPublisher.publishTeamInviteEvent(event);

        log.info("Invitation sent to userId: {} for startupId: {}",
                requestDto.getInvitedUserId(),
                requestDto.getStartupId());

        return invitationMapper.toResponseDto(savedInvitation);
    }

    // CANCEL INVITATION
    
    @Override
    public InvitationResponseDto cancelInvitation(Long invitationId,Long founderId) {

        // Find invitation
        Invitation invitation = invitationRepository
                .findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException(
                        "Invitation not found with id: " + invitationId));

        // Only founder who sent can cancel
        if (!invitation.getFounderId().equals(founderId)) {
            throw new UnauthorizedAccessException(
                    "You are not authorized to cancel this invitation");
        }

        // Only PENDING invitations can be cancelled
        if (!invitation.getStatus().equals(InvitationStatus.PENDING)) {
            throw new InvalidInvitationStatusException(
                    "Only PENDING invitations can be cancelled");
        }

        // Update status
        invitation.setStatus(InvitationStatus.CANCELLED);
        Invitation updatedInvitation = invitationRepository
                .save(invitation);

        log.info("Invitation cancelled for invitationId: {}",
                invitationId);

        return invitationMapper.toResponseDto(updatedInvitation);
    }

    // REJECT INVITATION

    @Override
    public InvitationResponseDto rejectInvitation(Long invitationId,
                                                   Long userId) {

        // Find invitation
        Invitation invitation = invitationRepository
                .findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException(
                        "Invitation not found with id: " + invitationId));

        // Only invited user can reject
        if (!invitation.getInvitedUserId().equals(userId)) {
            throw new UnauthorizedAccessException(
                    "You are not authorized to reject this invitation");
        }

        // Only PENDING invitations can be rejected
        if (!invitation.getStatus().equals(InvitationStatus.PENDING)) {
            throw new InvalidInvitationStatusException(
                    "Only PENDING invitations can be rejected");
        }

        // Update status
        invitation.setStatus(InvitationStatus.REJECTED);
        Invitation updatedInvitation = invitationRepository
                .save(invitation);

        log.info("Invitation rejected by userId: {} for invitationId: {}",
                userId, invitationId);

        return invitationMapper.toResponseDto(updatedInvitation);
    }

    // GET INVITATIONS BY USER ID

    @Override
    public List<InvitationResponseDto> getInvitationsByUserId(Long userId) {

        return invitationRepository
                .findByInvitedUserId(userId)
                .stream()
                .map(invitationMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // GET INVITATIONS BY STARTUP ID
    
    @Override
    public List<InvitationResponseDto> getInvitationsByStartupId(
            Long startupId,Long founderId) {
    	
    	verifyFounderOwnsStartup(
                startupId, founderId);

        return invitationRepository
                .findByStartupId(startupId)
                .stream()
                .map(invitationMapper::toResponseDto)
                .collect(Collectors.toList());
    }
    
    public void verifyFounderOwnsStartup(
            Long startupId,
            Long founderId) {

        // Call Startup Service
        StartupResponseDto startup = startupServiceClient
                .getStartupById(startupId);

        // Startup not found
        if (startup == null) {
            throw new StartupNotFoundException(
                    "Startup not found with id: " + startupId);
        }

        // Founder does not own startup
        if (!startup.getFounderId().equals(founderId)) {
            throw new ForbiddenAccessException(
                    "You are not authorized to " +
                    "perform this action on this startup");
        }
    }
}