package com.founderlink.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.founderlink.payment.dto.request.ConfirmPaymentRequest;
import com.founderlink.payment.dto.request.CreateOrderRequest;
import com.founderlink.payment.dto.response.ApiResponse;
import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.exception.AccessDeniedException;
import com.founderlink.payment.exception.PaymentNotFoundException;
import com.founderlink.payment.mapper.PaymentMapper;
import com.founderlink.payment.repository.PaymentRepository;
import com.founderlink.payment.service.RazorpayService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "APIs for managing payments")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    /**
     * POST /payments/create-order
     * Create Razorpay order for approved investment (user-initiated).
     * Auth: INVESTOR only + must own the investment.
     */
    @Operation(summary = "Create Razorpay order", description = "Creates a Razorpay order for an approved investment. Accessible to investors who own the investment.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Razorpay order created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request — Invalid input parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — Access denied to non-investors or unowned investments"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found — Payment not found for the investment")
    })
    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("POST /payments/create-order - userId: {}, investmentId: {}",
                userId, request.getInvestmentId());

        requireInvestor(userRole);

        Payment payment = paymentRepository.findByInvestmentId(request.getInvestmentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for investment: " + request.getInvestmentId()));
        requireOwner(payment, userId);

        CreateOrderResponse response = razorpayService.createOrder(
                request.getInvestmentId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                        "Razorpay order created successfully",
                        response));
    }

    /**
     * POST /payments/confirm
     * Confirm payment after Razorpay checkout success.
     * Auth: INVESTOR only + must own the payment.
     */
    @Operation(summary = "Confirm payment", description = "Confirms payment after Razorpay checkout success. Accessible to investors who own the payment.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment confirmed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request — Invalid input parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — Access denied to non-investors or unowned payments"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found — Payment not found for the order")
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmPaymentResponse>> confirmPayment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody ConfirmPaymentRequest request) {

        log.info("POST /payments/confirm - userId: {}, orderId: {}",
                userId, request.getRazorpayOrderId());

        requireInvestor(userRole);

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for order: " + request.getRazorpayOrderId()));
        requireOwner(payment, userId);

        ConfirmPaymentResponse response = razorpayService.confirmPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Payment confirmed successfully",
                        response));
    }

    /**
     * GET /payments/{paymentId}
     * Retrieve payment details.
     * Auth: must own the payment.
     */
    @Operation(summary = "Get payment details", description = "Retrieves details of a specific payment. Accessible to the owner of the payment.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — Access denied to unowned payments"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found — Payment not found")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPayment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long paymentId) {

        log.info("GET /payments/{} - userId: {}", paymentId, userId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        requireOwner(payment, userId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Payment retrieved successfully",
                        paymentMapper.toResponseDto(payment)));
    }

    /**
     * GET /payments/investment/{investmentId}
     * Retrieve payment by investment ID.
     * Auth: must own the payment.
     */
    @Operation(summary = "Get payment by investment", description = "Retrieves payment details associated with a specific investment ID. Accessible to the owner of the payment.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — Access denied to unowned payments"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found — Payment not found for the investment")
    })
    @GetMapping("/investment/{investmentId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentByInvestment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long investmentId) {

        log.info("GET /payments/investment/{} - userId: {}", investmentId, userId);

        Payment payment = paymentRepository.findByInvestmentId(investmentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for investment: " + investmentId));
        requireOwner(payment, userId);

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Payment retrieved successfully",
                        paymentMapper.toResponseDto(payment)));
    }

    // ── Authorization helpers ──────────────────────────────────────────────

    private void requireInvestor(String role) {
        if (!"ROLE_INVESTOR".equals(role)) {
            throw new AccessDeniedException("Only investors can make payments");
        }
    }

    private void requireOwner(Payment payment, Long userId) {
        if (!payment.getInvestorId().equals(userId)) {
            throw new AccessDeniedException("You do not own this investment");
        }
    }
}
