package com.tinder.swipe.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security.gateway-auth")
public record GatewayAuthProperties(
        boolean enabled,
        String headerName,
        String expectedValue,
        String userIdHeader,
        String internalPathPrefix,
        List<String> publicPathPrefixes
) {
    public GatewayAuthProperties {
        if (publicPathPrefixes == null) {
            publicPathPrefixes = List.of();
        }
    }
}
