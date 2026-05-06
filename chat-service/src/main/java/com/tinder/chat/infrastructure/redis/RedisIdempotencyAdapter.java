package com.tinder.chat.infrastructure.redis;

import com.tinder.chat.chat.port.IdempotencyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final String PENDING_MARKER = "PENDING";
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryAcquire(String idempotencyKey, Duration ttl) {
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(idempotencyKey, PENDING_MARKER, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void complete(String idempotencyKey, String resultValue, Duration ttl) {
        stringRedisTemplate.opsForValue().set(idempotencyKey, resultValue, ttl);
    }

    @Override
    public String getResult(String idempotencyKey) {
        return stringRedisTemplate.opsForValue().get(idempotencyKey);
    }
}