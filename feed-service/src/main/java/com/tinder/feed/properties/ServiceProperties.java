package com.tinder.feed.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.services")
public record ServiceProperties(
        String profileUrl
) {
}
