package com.founderlink.payment.entity;

/**
 * Payment lifecycle states for Razorpay integration.
 *
 * States:
 * - PENDING: Investment approved, awaiting user to initiate payment
 * - INITIATED: Razorpay order created, awaiting user payment
 * - SUCCESS: Payment captured by Razorpay, funds transferred
 * - FAILED: Payment operation failed
 */
public enum PaymentStatus {
    PENDING,     // Investment approved, payment not yet initiated
    INITIATED,   // Order created, awaiting payment
    SUCCESS,     // Payment captured and transferred
    FAILED       // Payment failed
}
