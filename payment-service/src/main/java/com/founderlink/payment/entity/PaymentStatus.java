package com.founderlink.payment.entity;

/**
 * Payment lifecycle states for saga orchestration.
 *
 * States:
 * - PENDING_HOLD: Awaiting hold authorization from investor creation
 * - HELD: Funds authorized but not yet charged
 * - CAPTURED: Funds actually charged to investor
 * - TRANSFERRED: Successfully transferred to startup wallet
 * - RELEASED: Funds released (rejection or compensation)
 * - FAILED: Payment operation failed
 */
public enum PaymentStatus {
    PENDING_HOLD,    // Initial state when investment created
    HELD,            // Stripe authorization successful
    CAPTURED,        // Stripe capture successful, investor charged
    TRANSFERRED,     // Successfully deposited to startup wallet
    RELEASED,        // Funds released (rejection or compensation)
    FAILED           // Payment failed at some step
}
