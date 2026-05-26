package com.tinder.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.notification.entity.DeviceToken;
import com.tinder.notification.entity.Notification;
import com.tinder.notification.enums.DeviceType;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.event.ActivityType;
import com.tinder.notification.event.UserActivityEvent;
import com.tinder.notification.repository.DeviceTokenRepository;
import com.tinder.notification.repository.InboxEventRepository;
import com.tinder.notification.repository.NotificationRepository;
import com.tinder.notification.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserActivityListener — Integration Tests")
class UserActivityListenerIT extends BaseIT {

    private static final String ACTIVITY_TOPIC = "user-activity-notification-it";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Value("${app.kafka.topics.user-activity}")
    private String userActivityTopic;

    @DynamicPropertySource
    static void kafkaTopics(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.topics.user-activity", () -> ACTIVITY_TOPIC);
        registry.add(
                "app.kafka.consumer-groups.notification-service",
                () -> "notification-user-activity-it-group");
        registry.add(
                "spring.kafka.producer.value-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
    }

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        deviceTokenRepository.deleteAll();
        inboxEventRepository.deleteAll();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("DELETE_ACCOUNT removes notifications and device tokens")
    void deleteAccount_RemovesUserData() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        notificationRepository.save(Notification.builder()
                .userId(userId)
                .title("Match")
                .body("Hello")
                .type(NotificationType.MATCH)
                .build());

        deviceTokenRepository.save(DeviceToken.builder()
                .userId(userId)
                .token("token-" + userId)
                .deviceType(DeviceType.ANDROID)
                .build());

        UserActivityEvent event =
                new UserActivityEvent(eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());
        kafkaTemplate
                .send(userActivityTopic, userId.toString(), objectMapper.writeValueAsString(event))
                .get();

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    assertEquals(0, notificationRepository.countByUserId(userId),
                            "Notifications for deleted user should be 0");
                    assertEquals(0, deviceTokenRepository.countByUserId(userId),
                            "Device tokens for deleted user should be 0");
                });

        assertTrue(inboxEventRepository.existsById(eventId));
    }
}
