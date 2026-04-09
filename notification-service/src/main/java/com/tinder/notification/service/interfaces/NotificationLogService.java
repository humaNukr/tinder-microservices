package com.tinder.notification.service.interfaces;

import com.tinder.notification.dto.NotificationResponseDto;

import java.util.List;
import java.util.UUID;

public interface NotificationLogService {

    Integer getUnreadCount(UUID userId);

    List<NotificationResponseDto> getUserNotifications(UUID userId);

    void markAsRead(UUID notificationId, UUID userId);
}
