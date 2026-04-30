package com.tinder.swipe.service.impl;

import com.tinder.swipe.exception.TooManyRequestsException;
import com.tinder.swipe.properties.RedisPrefixesProperties;
import com.tinder.swipe.properties.SwipeRateLimitProperties;
import com.tinder.swipe.service.interfaces.SwipeRateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SwipeRateLimiterServiceImpl implements SwipeRateLimiterService {
    private final RedisPrefixesProperties redisPrefixesProperties;
    private final SwipeRateLimitProperties swipeRateLimitProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void checkAndIncrementLikeLimit(UUID userId, boolean isPremium) {
        if (isPremium) {
            return;
        }

        String key = redisPrefixesProperties.limitKeyPrefix() + userId.toString();

        Long currentLikes = stringRedisTemplate.opsForValue().increment(key);

        if (currentLikes != null && currentLikes == 1L) {
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
        }

        if (currentLikes != null && currentLikes > swipeRateLimitProperties.maxLikesPerDay()) {
            throw new TooManyRequestsException("You have exceeded your daily like limit.");
        }
    }
}
