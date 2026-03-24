package com.founderlink.payment.client;

import java.math.BigDecimal;

/**
 * Stripe payment gateway implementation (placeholder).
 * To be implemented when Stripe API keys are available.
 * 
 * Current implementation: delegates to MockPaymentGatewayImpl
 * Future: replace with actual Stripe API calls
 */
public class StripePaymentGatewayImpl implements PaymentGatewayClient {

    private final MockPaymentGatewayImpl mockGateway;

    public StripePaymentGatewayImpl(MockPaymentGatewayImpl mockGateway) {
        this.mockGateway = mockGateway;
    }

    @Override
    public String holdFunds(
            Long investorId,
            BigDecimal amount,
            String idempotencyKey,
            String description) throws Exception {
        // TODO: Replace with actual Stripe call
        // return stripe.paymentIntents.create().authorize()
        return mockGateway.holdFunds(investorId, amount, idempotencyKey, description);
    }

    @Override
    public String captureFunds(
            String authorizationId,
            BigDecimal amount,
            String description) throws Exception {
        // TODO: Replace with actual Stripe call
        // return stripe.charges.create().capture()
        return mockGateway.captureFunds(authorizationId, amount, description);
    }

    @Override
    public void releaseFunds(
            String authorizationId,
            String reason) throws Exception {
        // TODO: Replace with actual Stripe call
        // stripe.paymentIntents.cancel(authorizationId)
        mockGateway.releaseFunds(authorizationId, reason);
    }

    @Override
    public boolean isAvailable() throws Exception {
        // TODO: Check actual Stripe API status
        return mockGateway.isAvailable();
    }
}
