package com.founderlink.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.founderlink.payment.dto.request.PaymentHoldRequestDto;
import com.founderlink.payment.dto.response.ApiResponse;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /payments/hold
     * Hold funds when investment is created (authorization step).
     */
    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> holdFunds(
            @Valid @RequestBody PaymentHoldRequestDto holdRequest) {

        log.info("POST /payments/hold - investmentId: {}, amount: ${}",
                holdRequest.getInvestmentId(), holdRequest.getAmount());

        PaymentResponseDto response = paymentService.holdFunds(holdRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "Funds held successfully",
                        response));
    }

    /**
     * PUT /payments/{paymentId}/capture
     * Capture held funds (actually charge investor).
     */
    @PutMapping("/{paymentId}/capture")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> captureFunds(
            @PathVariable Long paymentId) {

        log.info("PUT /payments/{}/capture", paymentId);

        PaymentResponseDto response = paymentService.captureFunds(paymentId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Funds captured successfully",
                        response));
    }

    /**
     * PUT /payments/{paymentId}/release
     * Release held funds (on rejection or compensation).
     */
    @PutMapping("/{paymentId}/release")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> releaseFunds(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String reason) {

        log.info("PUT /payments/{}/release - reason: {}", paymentId, reason);

        String releaseReason = reason != null ? reason : "No reason provided";
        PaymentResponseDto response = paymentService.releaseFunds(paymentId, releaseReason);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Funds released successfully",
                        response));
    }

    /**
     * GET /payments/{paymentId}
     * Retrieve payment details.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPayment(
            @PathVariable Long paymentId) {

        log.info("GET /payments/{}", paymentId);

        PaymentResponseDto response = paymentService.getPaymentById(paymentId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Payment retrieved successfully",
                        response));
    }

    /**
     * GET /payments/investment/{investmentId}
     * Retrieve payment by investment ID.
     */
    @GetMapping("/investment/{investmentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentByInvestment(
            @PathVariable Long investmentId) {

        log.info("GET /payments/investment/{}", investmentId);

        PaymentResponseDto response = paymentService.getPaymentByInvestmentId(investmentId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Payment retrieved successfully",
                        response));
    }
}
