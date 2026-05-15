package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisIdempotencyAdapterIT extends IntegrationTestBase {

    @Autowired
    private RedisIdempotencyAdapter adapter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void idempotencyFlow_WorksCorrectly() {
        String key = "idemp:" + UUID.randomUUID();
        Duration ttl = Duration.ofMinutes(1);

        assertTrue(adapter.tryAcquire(key, ttl));

        assertFalse(adapter.tryAcquire(key, ttl));

        String resultJson = "{\"status\":\"ok\"}";
        adapter.complete(key, resultJson, ttl);

        assertEquals(resultJson, adapter.getResult(key));

        assertFalse(adapter.tryAcquire(key, ttl));
    }
}