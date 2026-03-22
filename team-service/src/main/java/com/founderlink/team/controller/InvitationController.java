package com.founderlink.team.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.founderlink.team.dto.request.InvitationRequestDto;
import com.founderlink.team.dto.response.ApiResponse;
import com.founderlink.team.dto.response.InvitationResponseDto;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.service.InvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    // SEND INVITATION
    // POST /teams/invite
    // Called by → FOUNDER
    
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<?>> sendInvitation(
            @RequestHeader("X-User-Id") Long founderId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody InvitationRequestDto requestDto) {

        log.info("POST /teams/invite - sendInvitation by founderId: {}", founderId);
        // Throw exception instead of returning response
        if (!userRole.equals("ROLE_FOUNDER")) {
            log.warn("Access denied for sendInvitation - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only FOUNDERS can send invitations");
        }

        InvitationResponseDto response = invitationService
                .sendInvitation(founderId, requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "Invitation sent successfully",
                        response));
    }

    // CANCEL INVITATION
    // PUT /teams/invitations/{id}/cancel
    // Called by → FOUNDER
    
    @PutMapping("/invitations/{id}/cancel")
    public ResponseEntity<ApiResponse<?>> cancelInvitation(
            @RequestHeader("X-User-Id") Long founderId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long id) {

        log.info("PUT /teams/invitations/{}/cancel - founderId: {}", id, founderId);
        if (!userRole.equals("ROLE_FOUNDER")) {
            log.warn("Access denied for cancelInvitation - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only FOUNDERS can cancel invitations");
        }

        InvitationResponseDto response = invitationService
                .cancelInvitation(id, founderId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Invitation cancelled successfully",
                        response));
    }
    
    // REJECT INVITATION
    // PUT /teams/invitations/{id}/reject
    // Called by → CO-FOUNDER
    
    @PutMapping("/invitations/{id}/reject")
    public ResponseEntity<ApiResponse<?>> rejectInvitation(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long id) {

        log.info("PUT /teams/invitations/{}/reject - userId: {}", id, userId);
        if (!userRole.equals("ROLE_COFOUNDER")) {
            log.warn("Access denied for rejectInvitation - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only CO-FOUNDERS can reject invitations");
        }

        InvitationResponseDto response = invitationService
                .rejectInvitation(id, userId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Invitation rejected successfully",
                        response));
    }


    // GET INVITATIONS BY USER ID
    // GET /teams/invitations/user/{userId}
    // Called by → CO-FOUNDER
    
    @GetMapping("/invitations/user")
    public ResponseEntity<ApiResponse<?>> getInvitationsByUserId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {

        log.info("GET /teams/invitations/user - userId: {}", userId);
        if (!userRole.equals("ROLE_COFOUNDER")) {
            log.warn("Access denied for getInvitationsByUserId - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only CO-FOUNDERS can view their invitations");
        }

        List<InvitationResponseDto> response = invitationService
                .getInvitationsByUserId(userId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Invitations fetched successfully",
                        response));
    }
    
    // GET INVITATIONS BY STARTUP ID
    // GET /teams/invitations/startup/{startupId}
    // Called by → FOUNDER
    
    @GetMapping("/invitations/startup/{startupId}")
    public ResponseEntity<ApiResponse<?>> getInvitationsByStartupId(
            @RequestHeader("X-User-Id") Long founderId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long startupId) {

        log.info("GET /teams/invitations/startup/{} - founderId: {}", startupId, founderId);
        if (!userRole.equals("ROLE_FOUNDER")) {
            log.warn("Access denied for getInvitationsByStartupId - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only FOUNDERS can view startup invitations");
        }

        List<InvitationResponseDto> response = invitationService
                .getInvitationsByStartupId(startupId,founderId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Invitations fetched successfully",
                        response));
    }
}