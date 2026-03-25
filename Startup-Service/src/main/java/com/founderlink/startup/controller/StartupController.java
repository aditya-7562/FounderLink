package com.founderlink.startup.controller;

import java.math.BigDecimal;
import java.util.List;

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

import com.founderlink.startup.dto.request.StartupRequestDto;
import com.founderlink.startup.dto.response.ApiResponse;
import com.founderlink.startup.dto.response.StartupResponseDto;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.exception.ForbiddenAccessException;
import com.founderlink.startup.service.StartupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/startup")
@RequiredArgsConstructor
public class StartupController {

        private final StartupService startupService;

        // ─────────────────────────────────────────
        // CREATE STARTUP
        // POST /startup
        // Called by → FOUNDER
        // ─────────────────────────────────────────
        @PostMapping
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
        public ResponseEntity<ApiResponse<?>> getAllStartups(
                        @RequestHeader("X-User-Role") String userRole) {

                if (!userRole.equals("ROLE_INVESTOR") &&
                                !userRole.equals("ROLE_FOUNDER") &&
                                !userRole.equals("ROLE_COFOUNDER") &&
                                !userRole.equals("ROLE_ADMIN")) {
                        throw new ForbiddenAccessException(
                                        "Access denied");
                }

                log.info("GET /startup - get all the startups");
                List<StartupResponseDto> response = startupService
                                .getAllStartups();

                return ResponseEntity
                                .ok(new ApiResponse<>(
                                                "Startups fetched successfully",
                                                response));
        }

        // ─────────────────────────────────────────
        // GET STARTUP BY ID
        // GET /startup/{id}
        // Called by → ALL + FeignClient
        // No role check needed
        // ─────────────────────────────────────────
        @GetMapping("/{id}")
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
        public ResponseEntity<ApiResponse<?>> getStartupsByFounder(
                        @RequestHeader("X-User-Id") Long founderId,
                        @RequestHeader("X-User-Role") String userRole) {

                log.info("GET /startup/founder - founderId: {}, role: {}", founderId, userRole);
                if (!userRole.equals("ROLE_FOUNDER")) {
                        log.warn("Access denied for getStartupsByFounder - role: {}", userRole);
                        throw new ForbiddenAccessException(
                                        "Access denied. Only FOUNDERS " +
                                                        "can view their startups");
                }

                List<StartupResponseDto> response = startupService
                                .getStartupsByFounderId(founderId);

                return ResponseEntity
                                .ok(new ApiResponse<>(
                                                "Startups fetched successfully",
                                                response));
        }

        // ─────────────────────────────────────────
        // UPDATE STARTUP
        // PUT /startup/{id}
        // Called by → FOUNDER
        // ─────────────────────────────────────────
        @PutMapping("/{id}")
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
        public ResponseEntity<ApiResponse<?>> searchStartups(
                        @RequestHeader("X-User-Role") String userRole,
                        @RequestParam(required = false) String industry,
                        @RequestParam(required = false) StartupStage stage,
                        @RequestParam(required = false) BigDecimal minFunding,
                        @RequestParam(required = false) BigDecimal maxFunding) {

                log.info("GET /startup/search - industry: {}, stage: {}, role: {}", industry, stage, userRole);
                if (!userRole.equals("ROLE_INVESTOR") &&
                                !userRole.equals("ROLE_FOUNDER") &&
                                !userRole.equals("ROLE_COFOUNDER") &&
                                !userRole.equals("ROLE_ADMIN")) {
                        throw new ForbiddenAccessException(
                                        "Access denied");
                }

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
}