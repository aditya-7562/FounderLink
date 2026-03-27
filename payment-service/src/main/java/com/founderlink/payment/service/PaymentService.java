package com.founderlink.payment.service;

import com.founderlink.payment.dto.response.PaymentResponseDto;
import com.founderlink.payment.entity.PaymentStatus;

public interface PaymentService {

    /**
     * Retrieve payment by ID.
     */
    PaymentResponseDto getPaymentById(Long paymentId);

    /**
     * Retrieve payment by investment ID.
     */
    PaymentResponseDto getPaymentByInvestmentId(Long investmentId);

    /**
     * Get payment status.
     */
    PaymentStatus getPaymentStatus(Long paymentId);
}
