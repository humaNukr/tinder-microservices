package com.tinder.swipe.service.impl;

import com.tinder.swipe.exception.TooManyRequestsException;
import com.tinder.swipe.properties.RedisPrefixesProperties;
import com.tinder.swipe.properties.SwipeRateLimitProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SwipeRateLimiterServiceImpl")
class SwipeRateLimiterServiceImplTest {

    @Mock
    private RedisPrefixesProperties redisPrefixesProperties;
    @Mock
    private SwipeRateLimitProperties swipeRateLimitProperties;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SwipeRateLimiterServiceImpl rateLimiterService;

    @Test
    @DisplayName("throws TooManyRequestsException when daily limit exceeded")
    void checkAndIncrementLikeLimit_LimitExceeded_ThrowsTooManyRequests() {
        UUID userId = UUID.randomUUID();
        when(redisPrefixesProperties.limitKeyPrefix()).thenReturn("limit:");
        when(swipeRateLimitProperties.maxLikesPerDay()).thenReturn(2);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("limit:" + userId)).thenReturn(3L);

        assertThrows(TooManyRequestsException.class, () -> rateLimiterService.checkAndIncrementLikeLimit(userId, false));
    }

    @Test
    @DisplayName("sets TTL on first like of the day")
    void checkAndIncrementLikeLimit_FirstLike_SetsTtl() {
        UUID userId = UUID.randomUUID();
        when(redisPrefixesProperties.limitKeyPrefix()).thenReturn("limit:");
        when(swipeRateLimitProperties.maxLikesPerDay()).thenReturn(100);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("limit:" + userId)).thenReturn(1L);

        assertDoesNotThrow(() -> rateLimiterService.checkAndIncrementLikeLimit(userId, false));

        verify(stringRedisTemplate).expire(eq("limit:" + userId), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("skips rate limiting for premium users")
    void checkAndIncrementLikeLimit_PremiumUser_SkipsRedis() {
        rateLimiterService.checkAndIncrementLikeLimit(UUID.randomUUID(), true);

        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("allows swipe when Redis is unavailable")
    void checkAndIncrementLikeLimit_RedisDown_FailsOpen() {
        UUID userId = UUID.randomUUID();
        when(redisPrefixesProperties.limitKeyPrefix()).thenReturn("limit:");
        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

        assertDoesNotThrow(() -> rateLimiterService.checkAndIncrementLikeLimit(userId, false));
    }
}
