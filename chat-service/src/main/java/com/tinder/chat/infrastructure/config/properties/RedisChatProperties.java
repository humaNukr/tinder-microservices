package com.tinder.chat.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.redis.chat")
public record RedisChatProperties(
        String channel,
        String typingChannel,
        String readReceiptChannel,
        String reactionChannel,
        String deleteChannel,
        String editChannel,
        String keyPrefix,
        String keySuffix,
        Duration ttl
) {
}

