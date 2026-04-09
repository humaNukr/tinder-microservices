package com.tinder.notification.service.interfaces;

import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.enums.NotificationType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface NotificationLogService {

    Integer getUnreadCount(UUID userId);

    List<NotificationResponseDto> getUserNotifications(UUID userId);

    void markAsRead(UUID notificationId, UUID userId);

    void createNotification(UUID userId, String title, String body, NotificationType type, Map<String, Object> metadata);
}
