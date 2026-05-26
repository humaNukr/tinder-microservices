package com.tinder.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.notification.entity.DeviceToken;
import com.tinder.notification.enums.DeviceType;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.event.MatchEvent;
import com.tinder.notification.event.MessageSentEvent;
import com.tinder.notification.repository.DeviceTokenRepository;
import com.tinder.notification.repository.InboxEventRepository;
import com.tinder.notification.repository.NotificationRepository;
import com.tinder.notification.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Kafka Listeners — Integration Tests")
class KafkaListenersIT extends BaseIT {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Value("${app.kafka.topics.message-events}")
    private String messageEventsTopic;

    @Value("${app.kafka.topics.match-events}")
    private String matchEventsTopic;

    private UUID recipientId;
    private UUID senderId;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        notificationRepository.deleteAll();
        inboxEventRepository.deleteAll();
        deviceTokenRepository.deleteAll();
    }

    private MessageSentEvent messageEvent(UUID eventId, String contentType, String snippet) {
        return new MessageSentEvent(
                eventId,
                1L,
                UUID.randomUUID(),
                senderId,
                recipientId,
                contentType,
                snippet,
                Instant.now()
        );
    }

    private void registerDeviceToken(UUID userId, String token) {
        deviceTokenRepository.save(DeviceToken.builder()
                .userId(userId)
                .token(token)
                .deviceType(DeviceType.ANDROID)
                .build());
    }

    private void publishMessageEvent(MessageSentEvent event) throws Exception {
        kafkaTemplate.send(messageEventsTopic, recipientId.toString(), objectMapper.writeValueAsString(event)).get();
    }

    private void publishMatchEvent(MatchEvent event) throws Exception {
        kafkaTemplate.send(matchEventsTopic, event.eventId().toString(), objectMapper.writeValueAsString(event)).get();
    }

    @Test
    @DisplayName("MessageEventListener — persists MESSAGE notification with text snippet")
    void messageEvent_TextSnippet_CreatesInAppNotification() throws Exception {
        UUID eventId = UUID.randomUUID();
        publishMessageEvent(messageEvent(eventId, "TEXT", "Hello there"));

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            assertTrue(inboxEventRepository.existsById(eventId));
                assertEquals(1, notificationRepository.countByUserId(recipientId));
                var notification = notificationRepository.findAllByUserId(recipientId, PageRequest.of(0, 1))
                        .getContent().getFirst();
                assertEquals("Hello there", notification.getBody());
                assertEquals(NotificationType.MESSAGE, notification.getType());
        });
    }

    @Test
    @DisplayName("MessageEventListener — uses media placeholder body for non-text content")
    void messageEvent_MediaContent_UsesPlaceholderBody() throws Exception {
        UUID eventId = UUID.randomUUID();
        publishMessageEvent(messageEvent(eventId, "IMAGE", null));

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                assertEquals(
                        "📷 User sent a media file",
                        notificationRepository.findAllByUserId(recipientId, PageRequest.of(0, 1))
                                .getContent().getFirst().getBody()
                )
        );
    }

    @Test
    @DisplayName("MessageEventListener — duplicate event is ignored (idempotent inbox)")
    void messageEvent_DuplicateEvent_SecondDeliverySkipped() throws Exception {
        UUID eventId = UUID.randomUUID();
        MessageSentEvent event = messageEvent(eventId, "TEXT", "Once");

        publishMessageEvent(event);
        await().atMost(Duration.ofSeconds(15)).until(() -> inboxEventRepository.existsById(eventId));

        publishMessageEvent(event);

        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertEquals(1, notificationRepository.countByUserId(recipientId))
        );
    }

    @Test
    @DisplayName("MessageEventListener — saves in-app notification even when user has no device tokens")
    void messageEvent_NoDeviceTokens_StillPersistsNotification() throws Exception {
        publishMessageEvent(messageEvent(UUID.randomUUID(), "TEXT", "No push"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertEquals(1, notificationRepository.countByUserId(recipientId))
        );
    }

    @Test
    @DisplayName("MessageEventListener — processes event when user has registered device token")
    void messageEvent_WithDeviceToken_PersistsNotification() throws Exception {
        registerDeviceToken(recipientId, "fcm-token-123");
        UUID eventId = UUID.randomUUID();

        publishMessageEvent(messageEvent(eventId, "TEXT", "Ping"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertTrue(inboxEventRepository.existsById(eventId));
            assertEquals(1, notificationRepository.countByUserId(recipientId));
        });
    }

    @Test
    @DisplayName("MatchEventListener — creates MATCH notifications for both users")
    void matchEvent_ValidMatch_NotifiesBothUsers() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        publishMatchEvent(new MatchEvent(eventId, user1, user2));

        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            assertTrue(inboxEventRepository.existsById(eventId));
            assertAll(
                    () -> assertEquals(1, notificationRepository.countByUserId(user1)),
                    () -> assertEquals(1, notificationRepository.countByUserId(user2)),
                    () -> assertEquals(NotificationType.MATCH,
                            notificationRepository.findAllByUserId(user1, PageRequest.of(0, 1))
                                    .getContent().getFirst().getType())
            );
        });
    }

    @Test
    @DisplayName("MatchEventListener — duplicate match event does not create extra notifications")
    void matchEvent_DuplicateEvent_Idempotent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        MatchEvent event = new MatchEvent(eventId, user1, user2);

        publishMatchEvent(event);
        await().atMost(Duration.ofSeconds(15)).until(() -> inboxEventRepository.existsById(eventId));

        publishMatchEvent(event);

        await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertEquals(1, notificationRepository.countByUserId(user1))
        );
    }

    @Test
    @DisplayName("MatchEventListener — creates notifications for both users when tokens exist")
    void matchEvent_BothUsersHaveTokens_PersistsBothNotifications() throws Exception {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        registerDeviceToken(user1, "token-user-1");
        registerDeviceToken(user2, "token-user-2");

        publishMatchEvent(new MatchEvent(UUID.randomUUID(), user1, user2));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertAll(
                        () -> assertEquals(1, notificationRepository.countByUserId(user1)),
                        () -> assertEquals(1, notificationRepository.countByUserId(user2))
                )
        );
    }
}
