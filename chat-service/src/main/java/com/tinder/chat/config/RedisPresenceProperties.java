package com.tinder.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.redis.presence")
public record RedisPresenceProperties(
        String channel,
        String sessionKeyPrefix,
        String gracePeriodPrefix,
        Duration gracePeriodDuration
) {
}