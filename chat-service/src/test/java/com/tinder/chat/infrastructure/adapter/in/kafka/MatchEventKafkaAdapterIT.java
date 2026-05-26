package com.tinder.chat.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.in.room.CreateChatUseCase;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import com.tinder.chat.shared.dto.event.MatchEvent;
import com.tinder.chat.util.KafkaIntegrationTestBase;
import com.tinder.chat.util.KafkaListenerAwait;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class MatchEventKafkaAdapterIT extends KafkaIntegrationTestBase {

    private static final String MATCH_TOPIC = "match-events-chat-it";
    private static final String CONSUMER_GROUP = "match-events-chat-it-group";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private InboxEventJpaRepository inboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private CreateChatUseCase createChatUseCase;

    @Value("${app.kafka.topics.match-events}")
    private String topic;

    @DynamicPropertySource
    static void kafkaIntegration(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.topics.match-events", () -> MATCH_TOPIC);
        registry.add("app.kafka.consumer-groups.chat-service", () -> CONSUMER_GROUP);
        registry.add(
                "spring.kafka.producer.value-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
    }

    @BeforeEach
    void setUp() {
        inboxEventRepository.deleteAll();
        KafkaListenerAwait.awaitAssignment(applicationContext, CONSUMER_GROUP);
    }

    @Nested
    class ListenAndProcessMatchEvent {

        @Test
        void listenAndProcessMatchEvent_ValidEvent_SavesInboxAndTriggersUseCase() throws Exception {
            UUID eventId = UUID.randomUUID();
            UUID user1Id = UUID.randomUUID();
            UUID user2Id = UUID.randomUUID();
            MatchEvent event = new MatchEvent(eventId, user1Id, user2Id);
            String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, eventId.toString(), eventJson).get();

            await()
                    .pollInterval(Duration.ofMillis(200))
                    .atMost(Duration.ofSeconds(30))
                    .untilAsserted(() -> {
                        assertTrue(inboxEventRepository.existsByEventId(eventId));
                        verify(createChatUseCase).createChat(user1Id, user2Id);
                    });
        }
    }
}
