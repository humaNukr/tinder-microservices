package com.tinder.swipe.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.swipe.event.ActivityType;
import com.tinder.swipe.event.UserActivityEvent;
import com.tinder.swipe.repository.InboxEventRepository;
import com.tinder.swipe.repository.SwipeRepository;
import com.tinder.swipe.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("UserActivityListener — Integration Tests")
class UserActivityListenerIT extends BaseIT {

    private static final String ACTIVITY_TOPIC = "user-activity-swipe-it";
    private static final String CONSUMER_GROUP = "swipe-user-activity-it-group";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Value("${app.kafka.topics.user-activity}")
    private String userActivityTopic;

    @DynamicPropertySource
    static void kafkaTopics(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.topics.user-activity", () -> ACTIVITY_TOPIC);
        registry.add("spring.kafka.consumer.group-id", () -> CONSUMER_GROUP);
        registry.add(
                "spring.kafka.producer.value-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.listener.auto-startup", () -> "true");
    }

    @BeforeEach
    void setUp() {
        swipeRepository.deleteAll();
        inboxEventRepository.deleteAll();
        objectMapper.registerModule(new JavaTimeModule());

        await().atMost(Duration.ofSeconds(10))
                .until(() -> kafkaListenerEndpointRegistry.getListenerContainers().stream()
                        .anyMatch(MessageListenerContainer::isRunning));
    }

    @Test
    @DisplayName("DELETE_ACCOUNT is processed via Kafka and idempotent on replay")
    void deleteAccount_ProcessedAndIdempotent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UserActivityEvent event =
                new UserActivityEvent(eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());
        String payload = objectMapper.writeValueAsString(event);

        kafkaTemplate.send(userActivityTopic, userId.toString(), payload).get();
        await().atMost(Duration.ofSeconds(20)).until(() -> inboxEventRepository.existsById(eventId));

        kafkaTemplate.send(userActivityTopic, userId.toString(), payload).get();

        await().pollDelay(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertEquals(1, inboxEventRepository.count()));
    }
}
