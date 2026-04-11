package com.tinder.notification.processor;

import com.tinder.notification.entity.InboxEvent;
import com.tinder.notification.enums.MessageContentType;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.event.MessageSentEvent;
import com.tinder.notification.repository.InboxEventRepository;
import com.tinder.notification.service.impl.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageNotificationProcessor {

    private final InboxEventRepository inboxEventRepository;
    private final NotificationDeliveryService deliveryService;

    @Transactional
    public void process(MessageSentEvent event) {
        log.info("Processing message notification for event: {}", event.eventId());

        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.warn("Duplicate event detected (eventId: {}). Skipping.", event.eventId());
            return;
        }

        inboxEventRepository.save(new InboxEvent(event.eventId()));

        String title = "New message 💬";
        String body = determineBody(event);

        deliveryService.deliver(
                event.recipientId(),
                title,
                body,
                NotificationType.MESSAGE,
                Map.of(
                        "eventId", event.eventId(),
                        "messageId", event.id(),
                        "chatId", event.chatId(),
                        "senderId", event.senderId()
                )
        );
    }

    private String determineBody(MessageSentEvent event) {
        if (!MessageContentType.TEXT.name().equalsIgnoreCase(event.contentType())) {
            return "📷 User sent a media file";
        }

        String snippet = event.contentSnippet();
        return (snippet == null || snippet.isBlank()) ? "New message" : snippet;
    }
}
