package com.founderlink.team.controller;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.founderlink.team.dto.request.JoinTeamRequestDto;
import com.founderlink.team.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.founderlink.team.dto.response.TeamMemberResponseDto;
import com.founderlink.team.exception.ForbiddenAccessException;
import com.founderlink.team.service.TeamMemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Team Member", description = "APIs for managing team members")
public class TeamMemberController {

    private final TeamMemberService teamMemberService;

    // JOIN TEAM
    // POST /teams/join
    // Called by → CO-FOUNDER

    @Operation(summary = "Join a team", description = "Allows a user to join a team.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Joined team successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — invalid request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — COFOUNDER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Already a team member")
    })
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<?>> joinTeam(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody JoinTeamRequestDto requestDto) {

        log.info("POST /teams/join - userId: {}, role: {}", userId, userRole);
        if (!userRole.equals("ROLE_COFOUNDER")) {
            log.warn("Access denied for joinTeam - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only CO-FOUNDERS can join a team");
        }

        TeamMemberResponseDto response = teamMemberService
                .joinTeam(userId, requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "Successfully joined the team",
                        response));
    }

    // GET TEAM MEMBERS BY STARTUP ID
    // GET /teams/startup/{startupId}
    // Called by → ALL ROLES

    @Operation(summary = "Get team by startup ID", description = "Fetches the team for a given startup ID.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Team fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — not a team member or insufficient role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Startup not found")
    })
    @GetMapping("/startup/{startupId}")
    public ResponseEntity<?> getTeamByStartupId(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long startupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {

        log.info("GET /teams/startup/{} - userId: {}, role: {}", startupId, userId, userRole);
        // CoFounder membership check
        if (userRole.equals("ROLE_COFOUNDER")) {
            boolean isMember = teamMemberService
                    .isTeamMember(startupId, userId);
            if (!isMember) {
                throw new ForbiddenAccessException(
                        "Access denied. You are not a member of this startup");
            }
        }

        if (!userRole.equals("ROLE_FOUNDER") &&
            !userRole.equals("ROLE_COFOUNDER") &&
            !userRole.equals("ROLE_INVESTOR") &&
            !userRole.equals("ROLE_ADMIN")) {
            throw new ForbiddenAccessException(
                    "Access denied");
        }

        if (!isPaginatedRequest(request)) {
            List<TeamMemberResponseDto> response = teamMemberService
                    .getTeamByStartupId(startupId, userId, userRole);

            return ResponseEntity
                    .ok(new ApiResponse<>(
                            "Team members fetched successfully",
                            response));
        }

        Pageable pageable = buildPageable(page, size, sort, "joinedAt", Sort.Direction.DESC);
        return ResponseEntity.ok(toPaginatedResponse(teamMemberService.getTeamByStartupId(startupId, userId, userRole, pageable)));
    }

    // REMOVE TEAM MEMBER
    // DELETE /teams/{teamMemberId}
    // Called by → FOUNDER

    @Operation(summary = "Remove team member", description = "Removes a member from the team.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Team member removed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — FOUNDER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Team member not found")
    })
    @DeleteMapping("/{teamMemberId}")
    public ResponseEntity<ApiResponse<?>> removeTeamMember(
            @RequestHeader("X-User-Id") Long founderId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long teamMemberId) {

        log.info("DELETE /teams/{} - removeTeamMember by founderId: {}", teamMemberId, founderId);
        if (!userRole.equals("ROLE_FOUNDER")) {
            log.warn("Access denied for removeTeamMember - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only FOUNDERS can remove team members");
        }

        teamMemberService.removeTeamMember(
                teamMemberId, founderId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Team member removed successfully",
                        null));
    }

    // ─────────────────────────────────────────
    // GET MEMBER WORK HISTORY
    // GET /teams/member/history
    // Called by → CO-FOUNDER
    // ─────────────────────────────────────────

    @Operation(summary = "Get member history", description = "Fetches the history of a team member.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Member history fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — COFOUNDER or ADMIN role required")
    })
    @GetMapping("/member/history")
    public ResponseEntity<?> getMemberHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {

        log.info("GET /teams/member/history - userId: {}", userId);
        if (!userRole.equals("ROLE_COFOUNDER") &&
            !userRole.equals("ROLE_ADMIN")) {
            log.warn("Access denied for getMemberHistory - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied");
        }

        if (!isPaginatedRequest(request)) {
            List<TeamMemberResponseDto> response =
                    teamMemberService
                            .getMemberHistory(userId);

            return ResponseEntity
                    .ok(new ApiResponse<>(
                            "Member history fetched successfully",
                            response));
        }

        Pageable pageable = buildPageable(page, size, sort, "joinedAt", Sort.Direction.DESC);
        return ResponseEntity.ok(toPaginatedResponse(teamMemberService.getMemberHistory(userId, pageable)));
    }

    // ─────────────────────────────────────────
    // GET ACTIVE MEMBER ROLES
    // GET /teams/member/active
    // Called by → CO-FOUNDER
    // ─────────────────────────────────────────

    @Operation(summary = "Get active member roles", description = "Fetches active roles for a team member.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active member roles fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — COFOUNDER or ADMIN role required")
    })
    @GetMapping("/member/active")
    public ResponseEntity<?> getActiveMemberRoles(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            HttpServletRequest request) {

        log.info("GET /teams/member/active - userId: {}", userId);
        if (!userRole.equals("ROLE_COFOUNDER") &&
            !userRole.equals("ROLE_ADMIN")) {
            log.warn("Access denied for getActiveMemberRoles - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied");
        }

        if (!isPaginatedRequest(request)) {
            List<TeamMemberResponseDto> response =
                    teamMemberService
                            .getActiveMemberRoles(userId);

            return ResponseEntity
                    .ok(new ApiResponse<>(
                            "Active roles fetched successfully",
                            response));
        }

        Pageable pageable = buildPageable(page, size, sort, "joinedAt", Sort.Direction.DESC);
        return ResponseEntity.ok(toPaginatedResponse(teamMemberService.getActiveMemberRoles(userId, pageable)));
    }

    private boolean isPaginatedRequest(HttpServletRequest request) {
        return request.getParameterMap().containsKey("page");
    }

    private Pageable buildPageable(int page, int size, String sort, String defaultProperty, Sort.Direction defaultDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort resolvedSort = resolveSort(sort, defaultProperty, defaultDirection);
        return PageRequest.of(safePage, safeSize, resolvedSort);
    }

    private Sort resolveSort(String sort, String defaultProperty, Sort.Direction defaultDirection) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(defaultDirection, defaultProperty);
        }

        String[] tokens = sort.split(",");
        String property = tokens[0].isBlank() ? defaultProperty : tokens[0].trim();
        Sort.Direction direction = tokens.length > 1 && "desc".equalsIgnoreCase(tokens[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }

    private <T> Map<String, Object> toPaginatedResponse(Page<T> page) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", page.getContent());
        payload.put("page", page.getNumber());
        payload.put("size", page.getSize());
        payload.put("totalElements", page.getTotalElements());
        payload.put("totalPages", page.getTotalPages());
        payload.put("last", page.isLast());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", payload);
        response.put("error", null);
        return response;
    }
}
