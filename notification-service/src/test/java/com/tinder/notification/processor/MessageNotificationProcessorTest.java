package com.tinder.notification.processor;

import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.event.MessageSentEvent;
import com.tinder.notification.service.impl.InboxDedupService;
import com.tinder.notification.service.impl.NotificationDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageNotificationProcessor")
class MessageNotificationProcessorTest {

    @Mock
    private InboxDedupService inboxDedupService;

    @Mock
    private NotificationDeliveryService deliveryService;

    @InjectMocks
    private MessageNotificationProcessor processor;

    private UUID eventId;
    private UUID recipientId;
    private UUID senderId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        senderId = UUID.randomUUID();
    }

    private MessageSentEvent event(String contentType, String snippet) {
        return new MessageSentEvent(
                eventId,
                42L,
                UUID.randomUUID(),
                senderId,
                recipientId,
                contentType,
                snippet,
                Instant.now()
        );
    }

    @Nested
    @DisplayName("process()")
    class Process {

        @Test
        @DisplayName("delivers MESSAGE notification when event is new")
        void newEvent_DeliversNotification() {
            MessageSentEvent messageEvent = event("TEXT", "Hey!");
            when(inboxDedupService.tryRegister(eventId)).thenReturn(true);

            processor.process(messageEvent);

            verify(deliveryService).deliver(
                    eq(recipientId),
                    eq("New message 💬"),
                    eq("Hey!"),
                    eq(NotificationType.MESSAGE),
                    eq(Map.of(
                            "eventId", eventId,
                            "messageId", 42L,
                            "chatId", messageEvent.chatId(),
                            "senderId", senderId
                    ))
            );
        }

        @Test
        @DisplayName("skips delivery when inbox dedup rejects duplicate")
        void duplicateEvent_SkipsDelivery() {
            when(inboxDedupService.tryRegister(eventId)).thenReturn(false);

            processor.process(event("TEXT", "ignored"));

            verify(deliveryService, never()).deliver(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("body resolution")
    class BodyResolution {

        @Test
        @DisplayName("uses media placeholder for non-text content types")
        void mediaContent_UsesPlaceholder() {
            when(inboxDedupService.tryRegister(eventId)).thenReturn(true);
            MessageSentEvent messageEvent = event("IMAGE", null);

            processor.process(messageEvent);

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(deliveryService).deliver(
                    eq(recipientId),
                    eq("New message 💬"),
                    bodyCaptor.capture(),
                    eq(NotificationType.MESSAGE),
                    eq(Map.of(
                            "eventId", eventId,
                            "messageId", 42L,
                            "chatId", messageEvent.chatId(),
                            "senderId", senderId
                    ))
            );
            assertEquals("📷 User sent a media file", bodyCaptor.getValue());
        }

        @Test
        @DisplayName("falls back to default text when snippet is blank")
        void blankSnippet_UsesDefaultText() {
            when(inboxDedupService.tryRegister(eventId)).thenReturn(true);

            processor.process(event("TEXT", "   "));

            verify(deliveryService).deliver(
                    eq(recipientId),
                    eq("New message 💬"),
                    eq("New message"),
                    eq(NotificationType.MESSAGE),
                    any()
            );
        }
    }
}
