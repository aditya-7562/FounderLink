package com.founderlink.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.founderlink.payment.dto.request.ConfirmPaymentRequest;
import com.founderlink.payment.dto.request.CreateOrderRequest;
import com.founderlink.payment.dto.response.ApiResponse;
import com.founderlink.payment.dto.response.ConfirmPaymentResponse;
import com.founderlink.payment.dto.response.CreateOrderResponse;
import com.founderlink.payment.entity.Payment;
import com.founderlink.payment.exception.PaymentNotFoundException;
import com.founderlink.payment.repository.PaymentRepository;
import com.founderlink.payment.service.RazorpayService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final RazorpayService razorpayService;
    private final PaymentRepository paymentRepository;

    /**
     * POST /payments/create-order
     * Create Razorpay order for approved investment (user-initiated).
     * 
     * Required query params: amount, investorId, startupId, founderId
     * (These would normally come from investment-service, but for simplicity we pass them)
     */
    @PostMapping("/create-order")
public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
        @Valid @RequestBody CreateOrderRequest request) {

    log.info("POST /payments/create-order - investmentId: {}", 
            request.getInvestmentId());

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
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmPaymentResponse>> confirmPayment(
            @Valid @RequestBody ConfirmPaymentRequest request) {

        log.info("POST /payments/confirm - orderId: {}, paymentId: {}",
                request.getRazorpayOrderId(), request.getRazorpayPaymentId());

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
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<Payment>> getPayment(
            @PathVariable Long paymentId) {

        log.info("GET /payments/{}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Payment retrieved successfully",
                        payment));
    }

    /**
     * GET /payments/investment/{investmentId}
     * Retrieve payment by investment ID.
     */
    @GetMapping("/investment/{investmentId}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentByInvestment(
            @PathVariable Long investmentId) {

        log.info("GET /payments/investment/{}", investmentId);

        Payment payment = paymentRepository.findByInvestmentId(investmentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found for investment: " + investmentId));

        return ResponseEntity
                .ok(new ApiResponse<>(
                        "Payment retrieved successfully",
                        payment));
    }
}
