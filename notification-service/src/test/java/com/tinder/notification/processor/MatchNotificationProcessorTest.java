package com.tinder.notification.processor;

import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.service.impl.InboxDedupService;
import com.tinder.notification.service.impl.NotificationDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchNotificationProcessor")
class MatchNotificationProcessorTest {

    @Mock
    private InboxDedupService inboxDedupService;

    @Mock
    private NotificationDeliveryService deliveryService;

    @InjectMocks
    private MatchNotificationProcessor processor;

    private UUID eventId;
    private UUID user1Id;
    private UUID user2Id;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
    }

    @Nested
    @DisplayName("process()")
    class Process {

        @Test
        @DisplayName("notifies both users with swapped matchedUserId metadata")
        void newEvent_DeliversToBothUsers() {
            when(inboxDedupService.tryRegister(eventId)).thenReturn(true);

            processor.process(eventId, user1Id, user2Id);

            verify(deliveryService).deliver(
                    eq(user1Id),
                    eq("It's a Match! 💖"),
                    eq("You have a new match waiting for you."),
                    eq(NotificationType.MATCH),
                    eq(Map.of("eventId", eventId, "matchedUserId", user2Id))
            );
            verify(deliveryService).deliver(
                    eq(user2Id),
                    eq("It's a Match! 💖"),
                    eq("You have a new match waiting for you."),
                    eq(NotificationType.MATCH),
                    eq(Map.of("eventId", eventId, "matchedUserId", user1Id))
            );
        }

        @Test
        @DisplayName("skips delivery when event already registered")
        void duplicateEvent_NoDelivery() {
            when(inboxDedupService.tryRegister(eventId)).thenReturn(false);

            processor.process(eventId, user1Id, user2Id);

            verify(deliveryService, never()).deliver(any(), any(), any(), any(), any());
        }
    }
}
