package com.tinder.feed.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.feed")
public record FeedProperties(
        int pageSize,
        int deckSize,
        int fetchLimit,
        int refillThreshold
) {
}