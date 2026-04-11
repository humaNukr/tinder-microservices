package com.tinder.notification.dto;

import com.tinder.notification.enums.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponseDto(
        UUID id,
        String title,
        String body,
        NotificationType type,
        Map<String, Object> metadata,
        boolean isRead,
        Instant createdAt
) {
}