package com.tinder.notification.controller;

import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.dto.UnreadCountResponse;
import com.tinder.notification.service.interfaces.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogService notificationLogService;

    @GetMapping
    public Page<NotificationResponseDto> getUserNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return notificationLogService.getUserNotifications(userId, pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@RequestHeader("X-User-Id") UUID userId) {
        return new UnreadCountResponse(notificationLogService.getUnreadCount(userId));
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@RequestHeader("X-User-Id") UUID userId) {
        notificationLogService.markAllAsRead(userId);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(
            @PathVariable("id") UUID notificationId,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        notificationLogService.markAsRead(notificationId, userId);
    }
}
