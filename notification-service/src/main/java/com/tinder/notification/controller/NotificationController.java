package com.tinder.notification.controller;

import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.service.interfaces.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogService notificationLogService;

    @GetMapping
    public List<NotificationResponseDto> getUserNotifications(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        return notificationLogService.getUserNotifications(userId);
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
