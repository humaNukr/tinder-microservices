package com.tinder.auth.service.impl;

import com.tinder.auth.exception.TooManyRequestsException;
import com.tinder.auth.properties.RedisAuthProperties;
import com.tinder.auth.service.interfaces.OtpStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisOtpStorage implements OtpStorage {

    private final StringRedisTemplate redisTemplate;
    private final RedisAuthProperties props;

    @Override
    public void saveOtp(String identifier, String code) {
        String key = props.otpPrefix() + identifier;
        redisTemplate.opsForValue().set(key, code, props.otpTtl());
    }

    @Override
    public String getOtp(String identifier) {
        return redisTemplate.opsForValue().get(props.otpPrefix() + identifier);
    }

    @Override
    public void deleteOtp(String identifier) {
        redisTemplate.delete(props.otpPrefix() + identifier);
    }

    @Override
    public void checkAndIncrementRateLimit(String identifier) {
        String key = props.otpRateLimitPrefix() + identifier;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, props.otpRateLimitWindow());
        }
        if (count != null && count > props.otpRateLimitMaxRequests()) {
            throw new TooManyRequestsException("Too many requests for otp:" + identifier);
        }
    }

    @Override
    public void checkAndIncrementVerificationAttempts(String identifier) {
        String key = props.otpAttempts() + identifier;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, props.otpRateLimitWindow());
        }

        if (count != null && count > props.otpVerificationMaxAttempts()) {
            throw new TooManyRequestsException("Too many failed OTP attempts for: " + identifier);
        }
    }
}
