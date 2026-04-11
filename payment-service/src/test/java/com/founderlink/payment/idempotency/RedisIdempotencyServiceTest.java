package com.founderlink.payment.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisIdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void storeIdempotencyKey_Success() {
        String key = "test-key";
        Long paymentId = 123L;
        long ttl = 3600;

        idempotencyService.storeIdempotencyKey(key, paymentId, ttl);

        verify(valueOperations).set("idempotency:" + key, paymentId, ttl, TimeUnit.SECONDS);
    }

    @Test
    void getPaymentIdByIdempotencyKey_Found() {
        String key = "test-key";
        when(valueOperations.get("idempotency:" + key)).thenReturn(123L);

        Optional<Long> result = idempotencyService.getPaymentIdByIdempotencyKey(key);

        assertThat(result).isPresent().contains(123L);
    }

    @Test
    void getPaymentIdByIdempotencyKey_NotFound() {
        String key = "missing-key";
        when(valueOperations.get("idempotency:" + key)).thenReturn(null);

        Optional<Long> result = idempotencyService.getPaymentIdByIdempotencyKey(key);

        assertThat(result).isEmpty();
    }

    @Test
    void idempotencyKeyExists_ReturnsTrue() {
        String key = "test-key";
        when(redisTemplate.hasKey("idempotency:" + key)).thenReturn(true);

        boolean exists = idempotencyService.idempotencyKeyExists(key);

        assertThat(exists).isTrue();
    }

    @Test
    void removeIdempotencyKey_Success() {
        String key = "test-key";
        when(redisTemplate.delete("idempotency:" + key)).thenReturn(true);

        idempotencyService.removeIdempotencyKey(key);

        verify(redisTemplate).delete("idempotency:" + key);
    }

    @Test
    void getTimeToLive_Exists() {
        String key = "test-key";
        when(redisTemplate.getExpire("idempotency:" + key, TimeUnit.SECONDS)).thenReturn(100L);

        long ttl = idempotencyService.getTimeToLive(key);

        assertThat(ttl).isEqualTo(100L);
    }
}
