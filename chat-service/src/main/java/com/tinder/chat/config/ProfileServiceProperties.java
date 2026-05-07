package com.tinder.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.services.profile")
public record ProfileServiceProperties(
        String url
) {
}