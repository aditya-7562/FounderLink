package com.founderlink.payment.dlq;

import lombok.Getter;

/**
 * Custom exception for retryable failures in saga processing.
 * Used to trigger Spring Retry mechanism with exponential backoff.
 */
@Getter
public class RetryableException extends RuntimeException {

    private final String failureType;  // GATEWAY, SERVICE, DATABASE, NETWORK
    private final Object context;      // Payment ID, Investment ID, etc.

    public RetryableException(String message, String failureType, Object context) {
        super(message);
        this.failureType = failureType;
        this.context = context;
    }

    public RetryableException(String message, Throwable cause, String failureType, Object context) {
        super(message, cause);
        this.failureType = failureType;
        this.context = context;
    }

    @Override
    public String toString() {
        return String.format("RetryableException [failureType=%s, context=%s, message=%s]",
                failureType, context, getMessage());
    }
}
