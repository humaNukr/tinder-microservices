package com.tinder.swipe.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.swipe.rate-limit")
public record SwipeRateLimitProperties(
        int maxLikesPerDay
) {
}