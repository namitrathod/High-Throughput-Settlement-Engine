package com.faang.settlement_engine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_PREFIX = "payment_idempotency:";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    public boolean isRequestHandled(UUID key) {
        String status = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + key);
        return status != null && (status.equals(STATUS_PROCESSING) || status.equals(STATUS_COMPLETED));
    }

    public void markAsProcessing(UUID key) {
        redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + key, STATUS_PROCESSING, Duration.ofHours(24));
    }

    public void markAsCompleted(UUID key) {
        redisTemplate.opsForValue().set(IDEMPOTENCY_PREFIX + key, STATUS_COMPLETED, Duration.ofHours(24));
    }

    public String getStatus(UUID key) {
        return redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + key);
    }
}
