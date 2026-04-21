package com.tinder.feed.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.services.profile")
public record ProfileServiceProperties(
        String url,
        String candidatesPath,
        String batchPath
) {
}
