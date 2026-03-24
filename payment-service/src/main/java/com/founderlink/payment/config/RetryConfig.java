package com.founderlink.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry framework for the application.
 * 
 * @Retryable annotations on methods will use the configured retry policies.
 * Used in saga orchestrator for resilience against transient failures.
 * 
 * Retry Configuration:
 * - Max Attempts: 3 (initial + 2 retries)
 * - Backoff: Exponential (1s, 2s, 4s)
 * - Multiplier: 2.0 per retry
 * - Max Backoff: 8 seconds
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Configuration is handled via @Retryable annotations
    // and application.yml properties
}
