package com.tinder.notification.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        int defaultPageSize,
        int maxPageSize
) {
}
