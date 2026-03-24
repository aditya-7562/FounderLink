package com.founderlink.payment.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Mock implementation of payment gateway for testing.
 * Does not make real charges - simulates Stripe behavior.
 */
@Component
@Slf4j
public class MockPaymentGatewayImpl implements PaymentGatewayClient {

    @Override
    public String holdFunds(
            Long investorId,
            BigDecimal amount,
            String idempotencyKey,
            String description) throws Exception {

        // Simulate authorization - generates fake auth ID
        String authId = "auth_mock_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("MOCK: Holding funds for investor {}: ${} (auth: {})",
                investorId, amount, authId);

        // Simulate 50ms network call
        Thread.sleep(50);

        return authId;
    }

    @Override
    public String captureFunds(
            String authorizationId,
            BigDecimal amount,
            String description) throws Exception {

        // Simulate charge - generates fake charge ID
        String chargeId = "ch_mock_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("MOCK: Capturing funds with auth {}. Charge ID: {}",
                authorizationId, chargeId);

        // Simulate 100ms network call
        Thread.sleep(100);

        return chargeId;
    }

    @Override
    public void releaseFunds(
            String authorizationId,
            String reason) throws Exception {

        log.info("MOCK: Releasing funds from auth {}. Reason: {}",
                authorizationId, reason);

        // Simulate 50ms network call
        Thread.sleep(50);
    }

    @Override
    public boolean isAvailable() throws Exception {
        log.debug("MOCK: Payment gateway is available");
        return true;
    }
}
