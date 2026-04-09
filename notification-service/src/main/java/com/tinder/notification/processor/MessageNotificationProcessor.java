package com.tinder.notification.processor;

import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.service.impl.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageNotificationProcessor {

    private final NotificationDeliveryService deliveryService;

    public void process(UUID messageId, UUID chatId, UUID senderId, UUID recipientId, String textSnippet) {
        log.info("Processing message notification for chat: {}", chatId);

        String title = "New Message 💬";

        String body = textSnippet.length() > 30 ? textSnippet.substring(0, 30) + "..." : textSnippet;

        deliveryService.deliver(
                recipientId,
                title,
                body,
                NotificationType.MESSAGE,
                Map.of(
                        "messageId", messageId,
                        "chatId", chatId,
                        "senderId", senderId
                )
        );
    }
}