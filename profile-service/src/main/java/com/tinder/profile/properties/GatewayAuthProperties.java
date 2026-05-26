package com.tinder.profile.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.gateway-auth")
public record GatewayAuthProperties(
        boolean enabled,
        String headerName,
        String expectedValue,
        String userIdHeader,
        String internalPathPrefix
) {
}
