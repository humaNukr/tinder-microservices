package com.tinder.notification.processor;

import com.tinder.notification.entity.InboxEvent;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.repository.InboxEventRepository;
import com.tinder.notification.service.impl.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchNotificationProcessor {

    private final InboxEventRepository inboxEventRepository;
    private final NotificationDeliveryService deliveryService;

    public void process(UUID eventId, UUID user1Id, UUID user2Id) {
        log.info("Processing match notification for event: {}", eventId);

        if (inboxEventRepository.existsById(eventId)) {
            log.warn("Duplicate MatchEvent detected (eventId: {}). Skipping.", eventId);
            return;
        }

        inboxEventRepository.save(new InboxEvent(eventId));

        String title = "It's a Match! 💖";
        String body = "You have a new match waiting for you.";

        deliveryService.deliver(
                user1Id,
                title,
                body,
                NotificationType.MATCH,
                Map.of("eventId", eventId, "matchedUserId", user2Id)
        );

        deliveryService.deliver(
                user2Id,
                title,
                body,
                NotificationType.MATCH,
                Map.of("eventId", eventId, "matchedUserId", user1Id)
        );
    }
}