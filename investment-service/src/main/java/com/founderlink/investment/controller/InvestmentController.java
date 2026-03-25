package com.founderlink.investment.controller;

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

import com.founderlink.investment.dto.request.InvestmentRequestDto;
import com.founderlink.investment.dto.request.InvestmentStatusUpdateDto;
import com.founderlink.investment.dto.response.ApiResponse;
import com.founderlink.investment.dto.response.InvestmentResponseDto;
import com.founderlink.investment.exception.ForbiddenAccessException;
import com.founderlink.investment.service.InvestmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Slf4j
@RestController
@RequestMapping("/investments")
@RequiredArgsConstructor
@Tag(name = "Investments", description = "Endpoints for managing investments")
public class InvestmentController {

    private final InvestmentService investmentService;
    
    // CREATE INVESTMENT
    // POST /investments
    // Called by → INVESTOR
    
    @PostMapping
        @Operation(summary = "Create a new investment", description = "Creates a new investment. Only users with role INVESTOR can create investments.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Investment created successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
    public ResponseEntity<ApiResponse<?>> createInvestment(
            @RequestHeader("X-User-Id") Long investorId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody InvestmentRequestDto requestDto) {

        log.info("POST /investments - createInvestment by investorId: {}, role: {}", investorId, userRole);
        if (!userRole.equals("ROLE_INVESTOR")) {
            log.warn("Access denied for createInvestment - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only INVESTORS can create investments");
        }

        InvestmentResponseDto response = investmentService
                .createInvestment(investorId, requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "Investment created successfully",
                        response));
    }

    // GET INVESTMENTS BY STARTUP ID
    // GET /investments/startup/{startupId}
    // Called by → FOUNDER
    
    @GetMapping("/startup/{startupId}")
        @Operation(summary = "Get investments by startup ID", description = "Returns all investments for a given startup. Only users with role FOUNDER or ADMIN can access.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Investments fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
    public ResponseEntity<ApiResponse<?>> getInvestmentsByStartupId(
            @RequestHeader("X-User-Id") Long founderId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long startupId) {

        log.info("GET /investments/startup/{} - founderId: {}", startupId, founderId);
        if (!userRole.equals("ROLE_FOUNDER") &&
            !userRole.equals("ROLE_ADMIN")) {
            log.warn("Access denied for getInvestmentsByStartupId - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only FOUNDERS can view startup investments");
        }

        // TODO: FeignClient verify founder owns startup

        List<InvestmentResponseDto> response = investmentService
                .getInvestmentsByStartupId(startupId,founderId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Investments fetched successfully",
                        response));
    }

    // GET INVESTMENTS BY INVESTOR ID
    // GET /investments/investor
    // Called by → INVESTOR
    
    @GetMapping("/investor")
        @Operation(summary = "Get investments by investor ID", description = "Returns all investments for a given investor. Only users with role INVESTOR or ADMIN can access.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Investments fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
    public ResponseEntity<ApiResponse<?>> getInvestmentsByInvestorId(
            @RequestHeader("X-User-Id") Long investorId,
            @RequestHeader("X-User-Role") String userRole) {

        log.info("GET /investments/investor - investorId: {}", investorId);
        if (!userRole.equals("ROLE_INVESTOR") &&
            !userRole.equals("ROLE_ADMIN")) {
            log.warn("Access denied for getInvestmentsByInvestorId - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only INVESTORS can view their portfolio");
        }

        List<InvestmentResponseDto> response = investmentService
                .getInvestmentsByInvestorId(investorId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Investments fetched successfully",
                        response));
    }

    // UPDATE INVESTMENT STATUS
    // PUT /investments/{id}/status
    // Called by → FOUNDER
    
    @PutMapping("/{id}/status")
        @Operation(summary = "Update investment status", description = "Updates the status of an investment. Only users with role FOUNDER or ADMIN can update.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Investment status updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
    public ResponseEntity<ApiResponse<?>> updateInvestmentStatus(
            @RequestHeader("X-User-Id") Long founderId,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long id,
            @Valid @RequestBody InvestmentStatusUpdateDto statusUpdateDto) {

        log.info("PUT /investments/{}/status - founderId: {}, newStatus: {}", id, founderId, statusUpdateDto.getStatus());
        if (!userRole.equals("ROLE_FOUNDER") &&
            !userRole.equals("ROLE_ADMIN")) {
            log.warn("Access denied for updateInvestmentStatus - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied. Only FOUNDERS can update investment status");
        }

        // TODO: FeignClient verify founder owns startup

        InvestmentResponseDto response = investmentService
                .updateInvestmentStatus(id,founderId,statusUpdateDto);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Investment status updated successfully",
                        response));
    }
    
    // GET INVESTMENT BY ID
    // GET /investments/{id}
    // Called by → FOUNDER + INVESTOR
    
    @GetMapping("/{id}")
        @Operation(summary = "Get investment by ID", description = "Returns a single investment by its ID. Accessible by FOUNDER, INVESTOR, or ADMIN.")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Investment fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
        })
    public ResponseEntity<ApiResponse<?>> getInvestmentById(
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long id) {

        log.info("GET /investments/{} - getInvestmentById", id);
        if (!userRole.equals("ROLE_FOUNDER") &&
            !userRole.equals("ROLE_INVESTOR") &&
            !userRole.equals("ROLE_ADMIN")) {
            log.warn("Access denied for getInvestmentById - role: {}", userRole);
            throw new ForbiddenAccessException(
                    "Access denied");
        }

        InvestmentResponseDto response = investmentService
                .getInvestmentById(id);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Investment fetched successfully",
                        response));
    }
}