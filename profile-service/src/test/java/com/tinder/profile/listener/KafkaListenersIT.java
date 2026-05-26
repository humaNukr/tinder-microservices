package com.tinder.profile.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.profile.event.ActivityType;
import com.tinder.profile.event.UserActivityEvent;
import com.tinder.profile.event.UserPresenceEvent;
import com.tinder.profile.repository.InboxEventRepository;
import com.tinder.profile.repository.ProfileRepository;
import com.tinder.profile.util.BaseIT;
import com.tinder.profile.util.ProfileTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Kafka Listeners — Integration Tests")
class KafkaListenersIT extends BaseIT {

    private static final String ACTIVITY_TOPIC = "user-activity-listeners-it";
    private static final String PRESENCE_TOPIC = "user-presence-listeners-it";
    private static final String CONSUMER_GROUP = "profile-kafka-listeners-it-group";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Value("${app.kafka.topics.user-activity}")
    private String userActivityTopic;

    @Value("${app.kafka.topics.user-presence}")
    private String userPresenceTopic;

    @DynamicPropertySource
    static void kafkaTopics(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.topics.user-activity", () -> ACTIVITY_TOPIC);
        registry.add("app.kafka.topics.user-presence", () -> PRESENCE_TOPIC);
        registry.add("spring.kafka.consumer.group-id", () -> CONSUMER_GROUP);
    }

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        inboxEventRepository.deleteAll();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("UserActivityListener")
    class UserActivity {

        @Test
        @DisplayName("DELETE_ACCOUNT removes profile from database")
        void deleteAccount_RemovesProfile() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            ProfileTestFixtures.seedProfile(profileRepository, userId);

            UserActivityEvent event = new UserActivityEvent(
                    eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());
            kafkaTemplate.send(userActivityTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();

            await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(300)).untilAsserted(() ->
                    assertFalse(profileRepository.existsByUserId(userId))
            );

            assertTrue(inboxEventRepository.existsById(eventId));
        }

        @Test
        @DisplayName("duplicate DELETE_ACCOUNT is idempotent")
        void duplicateDelete_SkipsSecondProcessing() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UserActivityEvent event = new UserActivityEvent(
                    eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());

            kafkaTemplate.send(userActivityTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();
            await().atMost(Duration.ofSeconds(20)).until(() -> inboxEventRepository.existsById(eventId));

            kafkaTemplate.send(userActivityTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();

            await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertEquals(1, inboxEventRepository.count())
            );
        }

        @Test
        @DisplayName("LOGIN activity is ignored")
        void loginEvent_DoesNotDeleteProfile() throws Exception {
            UUID userId = UUID.randomUUID();
            ProfileTestFixtures.seedProfile(profileRepository, userId);

            UserActivityEvent event = new UserActivityEvent(
                    UUID.randomUUID(), userId, ActivityType.LOGIN, Instant.now());
            kafkaTemplate.send(userActivityTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();

            await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(8)).untilAsserted(() ->
                    assertTrue(profileRepository.existsByUserId(userId))
            );
        }
    }

    @Nested
    @DisplayName("UserPresenceListener")
    class UserPresence {

        @Test
        @DisplayName("updates lastSeen on presence event")
        void presenceEvent_UpdatesLastSeen() throws Exception {
            UUID userId = UUID.randomUUID();
            ProfileTestFixtures.seedProfile(profileRepository, userId);

            UserPresenceEvent event = new UserPresenceEvent(userId, true, Instant.parse("2026-03-01T08:00:00Z"));
            kafkaTemplate.send(userPresenceTopic, userId.toString(), objectMapper.writeValueAsString(event)).get();

            await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(300)).untilAsserted(() -> {
                var profile = profileRepository.findByUserId(userId).orElseThrow();
                assertTrue(profile.getLastSeen() != null);
            });
        }
    }
}
