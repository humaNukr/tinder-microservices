package com.tinder.notification.service.impl;

import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.entity.Notification;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.exception.AccessDeniedException;
import com.tinder.notification.exception.EntityNotFoundException;
import com.tinder.notification.mapper.NotificationMapper;
import com.tinder.notification.repository.NotificationRepository;
import com.tinder.notification.service.interfaces.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements NotificationLogService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public Integer getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUserNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(notificationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification with id: " + notificationId + " not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("Notification's userId and received UsedId don't match");
        }
        notification.setRead(true);
    }

    @Override
    @Transactional
    public void createNotification(UUID userId, String title, String body, NotificationType type, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .type(type)
                .metadata(metadata)
                .build();
        notificationRepository.save(notification);
    }
}
