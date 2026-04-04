package com.tinder.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.redis.chat")
public record RedisChatProperties(
        String channel,
        String keyPrefix,
        String keySuffix,
        Duration ttl
) {}

