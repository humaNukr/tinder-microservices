package com.tinder.chat.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.in.room.CreateChatUseCase;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import com.tinder.chat.shared.dto.event.MatchEvent;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class MatchEventKafkaAdapterIT extends IntegrationTestBase {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private InboxEventJpaRepository inboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateChatUseCase createChatUseCase;

    @Value("${app.kafka.topics.match-events}")
    private String topic;

    @BeforeEach
    void setUp() {
        inboxEventRepository.deleteAll();
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

            kafkaTemplate.send(topic, eventId.toString(), eventJson);

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertTrue(inboxEventRepository.existsByEventId(eventId));
                verify(createChatUseCase).createChat(user1Id, user2Id);
            });
        }
    }
}