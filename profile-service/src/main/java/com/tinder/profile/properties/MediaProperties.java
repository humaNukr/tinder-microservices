package com.tinder.profile.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.media")
public record MediaProperties(
        String publicPrefix,
        String maxSize
) {
}
