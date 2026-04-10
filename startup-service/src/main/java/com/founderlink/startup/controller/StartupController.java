package com.founderlink.startup.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.ApiResponse;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.exception.ForbiddenAccessException;
import com.founderlink.startup.service.StartupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/startup")
@RequiredArgsConstructor
@Tag(name = "Startup", description = "Endpoints for managing startups")
public class StartupController {

        private final StartupService startupService;

        // ─────────────────────────────────────────
        // CREATE STARTUP
        // POST /startup
        // Called by → FOUNDER
        // ─────────────────────────────────────────
        @PostMapping
        @Operation(summary = "Create a new startup", description = "Creates a new startup. Only users with role FOUNDER can create startups.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Startup created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — invalid request body"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — FOUNDER role required")
        })
        public ResponseEntity<ApiResponse<?>> createStartup(
                        @RequestHeader("X-User-Id") Long founderId,
                        @RequestHeader("X-User-Role") String userRole,
                        @Valid @RequestBody StartupRequestDto requestDto) {

                log.info("POST /startup - createStartup by founderId: {}, role: {}", founderId, userRole);

                if (!userRole.equals("ROLE_FOUNDER")) {
                        log.warn("Access denied for createStartup - role: {}", userRole);
                        throw new ForbiddenAccessException(
                                        "Access denied. Only FOUNDERS " +
                                                        "can create startups");
                }

                StartupResponseDto response = startupService
                                .createStartup(founderId, requestDto);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(new ApiResponse<>(
                                                "Startup created successfully",
                                                response));
        }

        // ─────────────────────────────────────────
        // GET ALL STARTUP
        // GET /startup
        // ─────────────────────────────────────────
        @GetMapping
        @Operation(summary = "Get all startups", description = "Returns all startups. Accessible by INVESTOR, FOUNDER, COFOUNDER, ADMIN.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Startups fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — insufficient role")
        })
        public ResponseEntity<ApiResponse<?>> getAllStartups(
                        @RequestHeader(value = "X-User-Role", required = false) String userRole,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String sort,
                        HttpServletRequest request) {

                if (StringUtils.hasText(userRole) &&
                                !userRole.equals("ROLE_INVESTOR") &&
                                !userRole.equals("ROLE_FOUNDER") &&
                                !userRole.equals("ROLE_COFOUNDER") &&
                                !userRole.equals("ROLE_ADMIN")) {
                        throw new ForbiddenAccessException(
                                        "Access denied");
                }

                log.info("GET /startup - get all the startups");
                if (!isPaginatedRequest(request)) {
                        List<StartupResponseDto> response = startupService
                                        .getAllStartups();

                        return ResponseEntity
                                        .ok(new ApiResponse<>(
                                                        "Startups fetched successfully",
                                                        response));
                }

                Pageable pageable = buildPageable(page, size, sort, "createdAt", Sort.Direction.DESC);
                return ResponseEntity.ok(toPaginatedResponse(startupService.getAllStartups(pageable)));
        }

        // ─────────────────────────────────────────
        // GET STARTUP BY ID
        // GET /startup/{id}
        // Called by → ALL + FeignClient
        // No role check needed
        // ─────────────────────────────────────────
        @GetMapping("/{id}")
        @Operation(summary = "Get startup by ID", description = "Returns a single startup by its ID. No role check needed.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Startup fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Startup not found")
        })
        public ResponseEntity<StartupResponseDto> getStartupById(
                        @PathVariable Long id) {

                log.info("GET /startup/{} - getStartupById", id);
                StartupResponseDto response = startupService
                                .getStartupById(id);

                return ResponseEntity
                                .ok(response);
        }

        // ─────────────────────────────────────────
        // GET STARTUP BY ID — FOR USERS
        // GET /startup/details/{id}
        // External use — returns ApiResponse
        // Called by Frontend
        // ─────────────────────────────────────────
        @GetMapping("/details/{id}")
        @Operation(summary = "Get startup details by ID", description = "Returns startup details for external use.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Startup fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Startup not found")
        })
        public ResponseEntity<ApiResponse<?>> getStartupDetails(
                        @PathVariable Long id) {

                log.info("GET /startup/details/{} - getStartupDetails", id);
                StartupResponseDto response = startupService
                                .getStartupById(id);

                return ResponseEntity
                                .ok(new ApiResponse<>(
                                                "Startup fetched successfully",
                                                response));
        }

        // ─────────────────────────────────────────
        // GET STARTUPS BY FOUNDER
        // GET /startup/founder
        // Called by → FOUNDER
        // ─────────────────────────────────────────
        @GetMapping("/founder")
        @Operation(summary = "Get startups by founder", description = "Returns all startups for a given founder. Only users with role FOUNDER can access.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Startups fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — FOUNDER role required")
        })
        public ResponseEntity<ApiResponse<?>> getStartupsByFounder(
                        @RequestHeader("X-User-Id") Long founderId,
                        @RequestHeader("X-User-Role") String userRole,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String sort,
                        HttpServletRequest request) {

                log.info("GET /startup/founder - founderId: {}, role: {}", founderId, userRole);
                if (!userRole.equals("ROLE_FOUNDER")) {
                        log.warn("Access denied for getStartupsByFounder - role: {}", userRole);
                        throw new ForbiddenAccessException(
                                        "Access denied. Only FOUNDERS " +
                                                        "can view their startups");
                }

                if (!isPaginatedRequest(request)) {
                        List<StartupResponseDto> response = startupService
                                        .getStartupsByFounderId(founderId);

                        return ResponseEntity
                                        .ok(new ApiResponse<>(
                                                        "Startups fetched successfully",
                                                        response));
                }

                Pageable pageable = buildPageable(page, size, sort, "createdAt", Sort.Direction.DESC);
                return ResponseEntity.ok(toPaginatedResponse(startupService.getStartupsByFounderId(founderId, pageable)));
        }

        // ─────────────────────────────────────────
        // UPDATE STARTUP
        // PUT /startup/{id}
        // Called by → FOUNDER
        // ─────────────────────────────────────────
        @PutMapping("/{id}")
        @Operation(summary = "Update a startup", description = "Updates a startup. Only users with role FOUNDER can update startups.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Startup updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — invalid request body"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — FOUNDER role required"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Startup not found")
        })
        public ResponseEntity<ApiResponse<?>> updateStartup(
                        @RequestHeader("X-User-Id") Long founderId,
                        @RequestHeader("X-User-Role") String userRole,
                        @PathVariable Long id,
                        @Valid @RequestBody StartupRequestDto requestDto) {

                log.info("PUT /startup/{} - updateStartup by founderId: {}", id, founderId);
                if (!userRole.equals("ROLE_FOUNDER")) {
                        log.warn("Access denied for updateStartup - role: {}", userRole);
                        throw new ForbiddenAccessException(
                                        "Access denied. Only FOUNDERS " +
                                                        "can update startups");
                }

                StartupResponseDto response = startupService
                                .updateStartup(id, founderId, requestDto);

                return ResponseEntity
                                .ok(new ApiResponse<>(
                                                "Startup updated successfully",
                                                response));
        }

        // ─────────────────────────────────────────
        // DELETE STARTUP
        // DELETE /startup/{id}
        // Called by → FOUNDER
        // ─────────────────────────────────────────
        @DeleteMapping("/{id}")
        @Operation(summary = "Delete a startup", description = "Deletes a startup. Only users with role FOUNDER can delete startups.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Startup deleted successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — FOUNDER role required"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Startup not found")
        })
        public ResponseEntity<ApiResponse<?>> deleteStartup(
                        @RequestHeader("X-User-Id") Long founderId,
                        @RequestHeader("X-User-Role") String userRole,
                        @PathVariable Long id) {

                log.info("DELETE /startup/{} - deleteStartup by founderId: {}", id, founderId);
                if (!userRole.equals("ROLE_FOUNDER")) {
                        log.warn("Access denied for deleteStartup - role: {}", userRole);
                        throw new ForbiddenAccessException(
                                        "Access denied. Only FOUNDERS " +
                                                        "can delete startups");
                }

                startupService.deleteStartup(id, founderId);

                return ResponseEntity
                                .ok(new ApiResponse<>(
                                                "Startup deleted successfully",
                                                null));
        }

        // ─────────────────────────────────────────
        // SEARCH STARTUPS
        // GET /startup/search
        // Called by → INVESTOR, FOUNDER, ADMIN
        // ─────────────────────────────────────────
        @GetMapping("/search")
        @Operation(summary = "Search startups", description = "Searches startups by industry, stage, and funding. Accessible by INVESTOR, FOUNDER, COFOUNDER, ADMIN.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Startups fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid search parameters or enum value"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — insufficient role")
        })
        public ResponseEntity<ApiResponse<?>> searchStartups(
                        @RequestHeader(value = "X-User-Role", required = false) String userRole,
                        @RequestParam(required = false) String industry,
                        @RequestParam(required = false) StartupStage stage,
                        @RequestParam(required = false) BigDecimal minFunding,
                        @RequestParam(required = false) BigDecimal maxFunding,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String sort,
                        HttpServletRequest request) {

                log.info("GET /startup/search - industry: {}, stage: {}, role: {}", industry, stage, userRole);
                if (StringUtils.hasText(userRole) &&
                                !userRole.equals("ROLE_INVESTOR") &&
                                !userRole.equals("ROLE_FOUNDER") &&
                                !userRole.equals("ROLE_COFOUNDER") &&
                                !userRole.equals("ROLE_ADMIN")) {
                        throw new ForbiddenAccessException(
                                        "Access denied");
                }

                if (!isPaginatedRequest(request)) {
                        List<StartupResponseDto> response = startupService
                                        .searchStartups(
                                                        industry,
                                                        stage,
                                                        minFunding,
                                                        maxFunding);

                        return ResponseEntity
                                        .ok(new ApiResponse<>(
                                                        "Startups fetched successfully",
                                                        response));
                }

        Pageable pageable = buildPageable(page, size, sort, "createdAt", Sort.Direction.DESC);
                return ResponseEntity.ok(toPaginatedResponse(startupService.searchStartups(
                                industry,
                                stage,
                                minFunding,
                                maxFunding,
                                pageable)));
        }

        // ─────────────────────────────────────────
        // GET PUBLIC STATS
        // GET /startup/public/stats
        // ─────────────────────────────────────────
        @GetMapping("/public/stats")
        @Operation(summary = "Get public platform stats", description = "Fetches the public count of startups and total funding.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stats fetched successfully")
        })
        public ResponseEntity<Map<String, Object>> getPublicStats() {
                return ResponseEntity.ok(startupService.getPublicStats());
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

        private <T> ApiResponse<Map<String, Object>> toPaginatedResponse(Page<T> page) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("content", page.getContent());
                payload.put("page", page.getNumber());
                payload.put("size", page.getSize());
                payload.put("totalElements", page.getTotalElements());
                payload.put("totalPages", page.getTotalPages());
                payload.put("last", page.isLast());

                return new ApiResponse<>("Startups fetched successfully", payload);
        }
}
