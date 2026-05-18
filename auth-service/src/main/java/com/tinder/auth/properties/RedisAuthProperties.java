package com.tinder.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.redis.auth")
public record RedisAuthProperties(String otpPrefix, String otpRateLimitPrefix, Duration otpTtl,
		int otpRateLimitMaxRequests, Duration otpRateLimitWindow, String sessionPrefix, String sessionSuffix) {
}
