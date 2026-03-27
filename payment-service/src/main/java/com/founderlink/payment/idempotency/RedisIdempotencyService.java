package com.founderlink.payment.idempotency;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis-based implementation of idempotency service.
 * 
 * Key format: "idempotency:{idempotencyKey}"
 * Value: Payment ID
 * TTL: Configurable, default 24 hours
 * 
 * Benefits:
 * - Distributed: Works across multiple payment service instances
 * - Fast: In-memory lookup vs database query
 * - Automatic expiry: Redis deletes expired keys automatically
 * - Reliable: Persisted (depends on Redis configuration)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final long DEFAULT_TTL_HOURS = 24;

    @Override
    public void storeIdempotencyKey(String idempotencyKey, Long paymentId, long ttlSeconds) {
        String redisKey = buildRedisKey(idempotencyKey);

        log.debug("Storing idempotency key in Redis: {}", idempotencyKey);

        redisTemplate.opsForValue().set(
                redisKey,
                paymentId,
                ttlSeconds,
                TimeUnit.SECONDS);

        log.debug("✓ Idempotency key stored: {}, TTL: {} seconds", idempotencyKey, ttlSeconds);
    }

    @Override
    public Optional<Long> getPaymentIdByIdempotencyKey(String idempotencyKey) {
        String redisKey = buildRedisKey(idempotencyKey);

        Object value = redisTemplate.opsForValue().get(redisKey);

        if (value != null) {
            Long paymentId = ((Number) value).longValue();
            log.debug("✓ Found payment {} for idempotency key: {}", paymentId, idempotencyKey);
            return Optional.of(paymentId);
        }

        log.debug("Idempotency key not found: {}", idempotencyKey);
        return Optional.empty();
    }

    @Override
    public boolean idempotencyKeyExists(String idempotencyKey) {
        String redisKey = buildRedisKey(idempotencyKey);
        Boolean exists = redisTemplate.hasKey(redisKey);
        return exists != null && exists;
    }

    @Override
    public void removeIdempotencyKey(String idempotencyKey) {
        String redisKey = buildRedisKey(idempotencyKey);

        Boolean deleted = redisTemplate.delete(redisKey);

        if (deleted != null && deleted) {
            log.debug("✓ Idempotency key removed: {}", idempotencyKey);
        } else {
            log.debug("Idempotency key not found for removal: {}", idempotencyKey);
        }
    }

    @Override
    public long getTimeToLive(String idempotencyKey) {
        String redisKey = buildRedisKey(idempotencyKey);
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }

    /**
     * Build Redis key format.
     */
    private String buildRedisKey(String idempotencyKey) {
        return IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
    }
}
