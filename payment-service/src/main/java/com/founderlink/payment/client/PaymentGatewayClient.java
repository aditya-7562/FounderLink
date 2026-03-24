package com.founderlink.payment.client;

import java.math.BigDecimal;

/**
 * Payment gateway interface for future implementation with Stripe/PayPal/etc.
 * Currently supports MockPaymentGateway for testing.
 */
public interface PaymentGatewayClient {

    /**
     * Hold funds (authorize without charging).
     * This is a two-step process where we confirm the investor has sufficient funds
     * but don't actually charge until founder approval.
     */
    String holdFunds(
            Long investorId,
            BigDecimal amount,
            String idempotencyKey,
            String description) throws Exception;

    /**
     * Capture funds (charge investor after founder approves).
     */
    String captureFunds(
            String authorizationId,
            BigDecimal amount,
            String description) throws Exception;

    /**
     * Release/Void funds (return to investor on rejection or failure).
     */
    void releaseFunds(
            String authorizationId,
            String reason) throws Exception;

    /**
     * Check if funds are available for investor.
     */
    boolean isAvailable() throws Exception;
}
