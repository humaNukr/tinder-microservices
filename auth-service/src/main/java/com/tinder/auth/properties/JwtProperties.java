package com.tinder.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Long accessTokenExpirationMs, Long refreshTokenExpirationMs) {
}
