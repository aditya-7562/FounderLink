package com.founderlink.payment.service;

import java.math.BigDecimal;

import com.founderlink.payment.dto.request.PaymentHoldRequestDto;
import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.PaymentStatus;

public interface PaymentService {

    /**
     * Hold funds when investment is created (authorize without charging).
     */
    PaymentResponseDto holdFunds(PaymentHoldRequestDto holdRequest);

    /**
     * Capture funds when founder approves investment (actual charge).
     */
    PaymentResponseDto captureFunds(Long paymentId);

    /**
     * Release funds when founder rejects investment (compensation/void auth).
     */
    PaymentResponseDto releaseFunds(Long paymentId, String reason);

    /**
     * Mark payment as transferred to startup wallet (final success state).
     */
    void markAsTransferred(Long paymentId, String walletTransactionId);

    /**
     * Mark payment as failed.
     */
    void markAsFailed(Long paymentId, String failureReason);

    /**
     * Retrieve payment by ID.
     */
    PaymentResponseDto getPaymentById(Long paymentId);

    /**
     * Retrieve payment by investment ID.
     */
    PaymentResponseDto getPaymentByInvestmentId(Long investmentId);

    /**
     * Check if payment with idempotency key already exists.
     */
    boolean paymentExists(String idempotencyKey);

    /**
     * Get payment status.
     */
    PaymentStatus getPaymentStatus(Long paymentId);
}
