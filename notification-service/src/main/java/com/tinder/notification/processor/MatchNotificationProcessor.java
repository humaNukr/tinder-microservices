package com.tinder.notification.processor;

import com.tinder.notification.provider.PushSender;
import com.tinder.notification.repository.projection.DeviceTokenInfo;
import com.tinder.notification.service.interfaces.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchNotificationProcessor {

    private final DeviceTokenService deviceTokenService;
    private final PushSender pushSender;

    public void process(UUID eventId, UUID user1Id, UUID user2Id) {
        log.info("Processing match notification for event: {}", eventId);

        notifyUser(user1Id, "It's a Match!", "You have a new match waiting for you.");
        notifyUser(user2Id, "It's a Match!", "You have a new match waiting for you.");
    }

    private void notifyUser(UUID userId, String title, String body) {
        List<DeviceTokenInfo> tokens = deviceTokenService.getUserTokens(userId);

        if (tokens.isEmpty()) {
            log.warn("No device tokens found for user: {}. Push skipped.", userId);
            return;
        }

        for (DeviceTokenInfo tokenInfo : tokens) {
            pushSender.sendNotification(tokenInfo.getToken(), title, body);
        }
    }
}