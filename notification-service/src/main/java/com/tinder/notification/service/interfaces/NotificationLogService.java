package com.tinder.notification.service.interfaces;

import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationLogService {

    Integer getUnreadCount(UUID userId);

    Page<NotificationResponseDto> getUserNotifications(UUID userId, Pageable pageable);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    void createNotification(UUID userId, String title, String body, NotificationType type, Map<String, Object> metadata);
}
