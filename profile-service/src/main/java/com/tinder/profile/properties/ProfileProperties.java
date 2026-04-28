package com.tinder.profile.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.profile")
public record ProfileProperties(
        int maxPhotos,
        int minAge,
        double defaultSearchRadiusKm
) {
}