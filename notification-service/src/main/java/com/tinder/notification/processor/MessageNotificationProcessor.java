package com.tinder.notification.processor;


import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.event.MessageEvent;
import com.tinder.notification.service.impl.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageNotificationProcessor {

    private final NotificationDeliveryService deliveryService;

    public void process(MessageEvent event) {
        log.info("Processing message notification for event: {}", event.eventId());

        String title = "New Message 💬";

        String snippet = event.contentSnippet();
        String body = snippet.length() > 50 ? snippet.substring(0, 50) + "..." : snippet;

        deliveryService.deliver(
                event.recipientId(),
                title,
                body,
                NotificationType.MESSAGE,
                Map.of(
                        "eventId", event.eventId(),
                        "messageId", event.messageId(),
                        "chatId", event.chatId(),
                        "senderId", event.senderId()
                )
        );
    }
}