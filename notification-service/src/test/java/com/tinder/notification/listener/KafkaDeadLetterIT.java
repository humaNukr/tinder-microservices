package com.tinder.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.notification.event.MatchEvent;
import com.tinder.notification.event.MessageSentEvent;
import com.tinder.notification.processor.MatchNotificationProcessor;
import com.tinder.notification.processor.MessageNotificationProcessor;
import com.tinder.notification.repository.NotificationRepository;
import com.tinder.notification.util.BaseIT;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@DisplayName("Kafka DLT — Integration Tests")
class KafkaDeadLetterIT extends BaseIT {

    private static final String MESSAGE_EVENTS_TOPIC = "message-events-dlt-it";
    private static final String MATCH_EVENTS_TOPIC = "match-events-dlt-it";
    private static final String CONSUMER_GROUP = "notification-service-dlt-it-group";

    private static final Duration DLT_WAIT = Duration.ofSeconds(45);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private MessageNotificationProcessor messageNotificationProcessor;

    @MockitoBean
    private MatchNotificationProcessor matchNotificationProcessor;

    @Value("${app.kafka.topics.message-events}")
    private String messageEventsTopic;

    @Value("${app.kafka.topics.match-events}")
    private String matchEventsTopic;

    private KafkaConsumer<String, String> dltConsumer;

    @DynamicPropertySource
    static void isolateKafka(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.topics.message-events", () -> MESSAGE_EVENTS_TOPIC);
        registry.add("app.kafka.topics.match-events", () -> MATCH_EVENTS_TOPIC);
        registry.add("app.kafka.consumer-groups.notification-service", () -> CONSUMER_GROUP);
    }

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reset(messageNotificationProcessor, matchNotificationProcessor);
    }

    @AfterEach
    void tearDown() {
        if (dltConsumer != null) {
            dltConsumer.close();
            dltConsumer = null;
        }
    }

    private void subscribeToDlt(String sourceTopic) {
        // Spring Kafka 3.x default DeadLetterPublishingRecoverer suffix
        String dltTopic = sourceTopic + "-dlt";
        dltConsumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-verifier-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()
        ));
        dltConsumer.subscribe(List.of(dltTopic));
    }

    private void awaitDltRecord(String sourceTopic, String expectedPayloadFragment) {
        await().atMost(DLT_WAIT).pollInterval(Duration.ofMillis(300)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = dltConsumer.poll(POLL_TIMEOUT);
            assertFalse(records.isEmpty(), "Expected record on " + sourceTopic + "-dlt");

            ConsumerRecord<String, String> record = records.iterator().next();
            assertTrue(record.value().contains(expectedPayloadFragment),
                    "DLT payload should contain the original message fragment");

            Header originalTopic = record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);
            assertNotNull(originalTopic, "DLT record must carry original topic header");
            assertEquals(sourceTopic, new String(originalTopic.value(), StandardCharsets.UTF_8));
        });
    }

    @Nested
    @DisplayName("message-events")
    class MessageEvents {

        @Test
        @DisplayName("publishes to DLT after retries when processing fails")
        void processingFailure_AfterRetries_PublishedToDlt() throws Exception {
            UUID eventId = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();
            String payload = objectMapper.writeValueAsString(new MessageSentEvent(
                    eventId, 1L, UUID.randomUUID(), UUID.randomUUID(), recipientId,
                    "TEXT", "fail", Instant.now()
            ));

            doThrow(new RuntimeException("simulated downstream failure"))
                    .when(messageNotificationProcessor).process(any(MessageSentEvent.class));

            subscribeToDlt(messageEventsTopic);
            kafkaTemplate.send(messageEventsTopic, recipientId.toString(), payload).get();

            awaitDltRecord(messageEventsTopic, eventId.toString());
            await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertEquals(0, notificationRepository.countByUserId(recipientId))
            );
        }

        @Test
        @DisplayName("publishes invalid JSON to DLT without persisting notification")
        void invalidJson_PublishedToDltImmediately() throws Exception {
            String payload = "{not-valid-json";
            String marker = "not-valid-json";

            subscribeToDlt(messageEventsTopic);
            kafkaTemplate.send(messageEventsTopic, UUID.randomUUID().toString(), payload).get();

            awaitDltRecord(messageEventsTopic, marker);
            assertEquals(0, notificationRepository.count());
        }
    }

    @Nested
    @DisplayName("match-events")
    class MatchEvents {

        @Test
        @DisplayName("publishes to DLT after retries when match processing fails")
        void processingFailure_AfterRetries_PublishedToDlt() throws Exception {
            UUID eventId = UUID.randomUUID();
            UUID user1 = UUID.randomUUID();
            UUID user2 = UUID.randomUUID();
            String payload = objectMapper.writeValueAsString(new MatchEvent(eventId, user1, user2));

            doThrow(new RuntimeException("simulated match failure"))
                    .when(matchNotificationProcessor).process(any(), any(), any());

            subscribeToDlt(matchEventsTopic);
            kafkaTemplate.send(matchEventsTopic, eventId.toString(), payload).get();

            awaitDltRecord(matchEventsTopic, eventId.toString());
            await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertEquals(0, notificationRepository.countByUserId(user1))
            );
        }
    }
}
