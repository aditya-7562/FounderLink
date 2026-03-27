package com.founderlink.payment.idempotency;

import java.util.Optional;

/**
 * Interface for idempotency key management.
 * Prevents duplicate payment processing in distributed systems.
 */
public interface IdempotencyService {

    /**
     * Store idempotency key with associated payment ID.
     * 
     * @param idempotencyKey Unique request identifier (UUID)
     * @param paymentId Associated payment ID
     * @param ttlSeconds Time-to-live in seconds (how long to keep in cache)
     */
    void storeIdempotencyKey(String idempotencyKey, Long paymentId, long ttlSeconds);

    /**
     * Check if idempotency key exists and retrieve associated payment ID.
     * 
     * @param idempotencyKey Unique request identifier
     * @return Optional with paymentId if key exists, empty if not found
     */
    Optional<Long> getPaymentIdByIdempotencyKey(String idempotencyKey);

    /**
     * Check if idempotency key exists (without retrieving payment ID).
     * 
     * @param idempotencyKey Unique request identifier
     * @return true if key exists, false otherwise
     */
    boolean idempotencyKeyExists(String idempotencyKey);

    /**
     * Remove idempotency key (after payment success or timeout).
     * 
     * @param idempotencyKey Unique request identifier
     */
    void removeIdempotencyKey(String idempotencyKey);

    /**
     * Get TTL remaining for idempotency key.
     * 
     * @param idempotencyKey Unique request identifier
     * @return TTL in seconds, -1 if key doesn't exist
     */
    long getTimeToLive(String idempotencyKey);
}
