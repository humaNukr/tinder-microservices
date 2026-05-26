package com.tinder.notification.service.impl;

import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.provider.PushSender;
import com.tinder.notification.repository.projection.DeviceTokenInfo;
import com.tinder.notification.service.interfaces.DeviceTokenService;
import com.tinder.notification.service.interfaces.NotificationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final NotificationLogService notificationLogService;
    private final DeviceTokenService deviceTokenService;
    private final PushSender pushSender;

    public void deliver(UUID userId, String title, String body, NotificationType type, Map<String, Object> metadata) {
        notificationLogService.createNotification(userId, title, body, type, metadata);
        log.info("Saved In-App notification for user {}", userId);

        sendPush(userId, title, body, metadata);
    }

    private void sendPush(UUID userId, String title, String body, Map<String, Object> metadata) {
        List<DeviceTokenInfo> tokens = deviceTokenService.getUserTokens(userId);

        if (tokens.isEmpty()) {
            log.warn("No device tokens found for user: {}. Push skipped, but In-App saved.", userId);
            return;
        }

        Map<String, String> stringData = metadata.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));

        for (DeviceTokenInfo tokenInfo : tokens) {
            try {
                pushSender.sendNotification(tokenInfo.getToken(), title, body, stringData);
            } catch (RuntimeException e) {
                log.error("Push failed for user {} token {}: {}", userId, tokenInfo.getToken(), e.getMessage(), e);
            }
        }
    }
}
