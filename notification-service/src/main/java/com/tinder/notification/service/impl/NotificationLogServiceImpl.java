package com.tinder.notification.service.impl;

import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.entity.Notification;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.exception.AccessDeniedException;
import com.tinder.notification.exception.EntityNotFoundException;
import com.tinder.notification.mapper.NotificationMapper;
import com.tinder.notification.properties.NotificationProperties;
import com.tinder.notification.repository.NotificationRepository;
import com.tinder.notification.service.interfaces.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationLogServiceImpl implements NotificationLogService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationProperties notificationProperties;

    @Override
    public Integer getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getUserNotifications(UUID userId, Pageable pageable) {
        Pageable normalized = normalizePageable(pageable);
        return notificationRepository.findAllByUserId(userId, normalized)
                .map(notificationMapper::toDto);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification with id: " + notificationId + " not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("Notification does not belong to the authenticated user");
        }
        notification.setRead(true);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    private Pageable normalizePageable(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), notificationProperties.maxPageSize());
        if (size <= 0) {
            size = notificationProperties.defaultPageSize();
        }
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
