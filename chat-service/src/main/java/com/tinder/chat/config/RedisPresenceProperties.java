package com.tinder.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis.presence")
public record RedisPresenceProperties(
        String channel,
        String sessionKeyPrefix
) {
}