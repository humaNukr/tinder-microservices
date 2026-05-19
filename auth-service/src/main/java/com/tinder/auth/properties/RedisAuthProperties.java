package com.tinder.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.redis.auth")
public record RedisAuthProperties(
        String otpPrefix, String otpRateLimitPrefix,
        String otpAttempts, Duration otpTtl,
        int otpRateLimitMaxRequests, Duration otpRateLimitWindow,
        int otpVerificationMaxAttempts, String sessionPrefix, String sessionSuffix
) {
}
