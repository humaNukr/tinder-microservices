package com.tinder.swipe.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis.prefixes")
public record RedisPrefixesProperties(
        String limitKeyPrefix
) {
}
