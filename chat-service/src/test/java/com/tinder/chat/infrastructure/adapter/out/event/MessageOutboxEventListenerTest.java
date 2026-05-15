package com.tinder.chat.infrastructure.adapter.out.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.event.MessageSavedEvent;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.outbox.OutboxJpaRepository;
import com.tinder.chat.infrastructure.config.properties.KafkaTopicsProperties;
import com.tinder.chat.shared.dto.event.MessageSentEvent;
import com.tinder.chat.shared.mapper.MessageEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageOutboxEventListenerTest {

    private final String topicName = "message-events-topic";
    @Mock
    private OutboxJpaRepository outboxRepository;
    @Mock
    private KafkaTopicsProperties kafkaTopicsProperties;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private MessageEventMapper messageEventMapper;
    @InjectMocks
    private MessageOutboxEventListener listener;
    @Captor
    private ArgumentCaptor<OutboxEventEntity> outboxEntityCaptor;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
    }

    private Message createMessage(String content) {
        return Message.builder()
                .id(1L)
                .chatId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .contentType(MessageContentType.TEXT)
                .content(content)
                .createdAt(Instant.now())
                .build();
    }

    private MessageSentEvent createMockKafkaEvent(String snippet) {
        return new MessageSentEvent(
                UUID.randomUUID(),
                1L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                recipientId,
                "TEXT",
                snippet,
                Instant.now()
        );
    }

    private void verifyAndAssertOutboxEntitySaved(String expectedJson) {
        verify(outboxRepository).save(outboxEntityCaptor.capture());
        OutboxEventEntity savedEntity = outboxEntityCaptor.getValue();
        assertEquals(topicName, savedEntity.getTopic());
        assertEquals(expectedJson, savedEntity.getPayload());
        assertNotNull(savedEntity.getCreatedAt());
    }

    @Nested
    class HandleMessageSavedEvent {

        @Test
        void handleMessageSavedEvent_ShortContent_SavesWithoutTruncation() throws JsonProcessingException {
            String shortContent = "Hello, how are you?";
            Message message = createMessage(shortContent);
            MessageSavedEvent event = new MessageSavedEvent(message, recipientId);
            MessageSentEvent mockKafkaEvent = createMockKafkaEvent(shortContent);
            String mockJson = "{}";

            when(messageEventMapper.toMessageSentEvent(eq(message), any(UUID.class), eq(recipientId), eq(shortContent)))
                    .thenReturn(mockKafkaEvent);
            when(kafkaTopicsProperties.messageEvents()).thenReturn(topicName);
            when(objectMapper.writeValueAsString(mockKafkaEvent)).thenReturn(mockJson);

            listener.handleMessageSavedEvent(event);

            verifyAndAssertOutboxEntitySaved(mockJson);
        }

        @Test
        void handleMessageSavedEvent_LongContent_SavesWithTruncatedSnippet() throws JsonProcessingException {
            String longContent = "A".repeat(100);
            String expectedSnippet = "A".repeat(50) + "...";
            Message message = createMessage(longContent);
            MessageSavedEvent event = new MessageSavedEvent(message, recipientId);
            MessageSentEvent mockKafkaEvent = createMockKafkaEvent(expectedSnippet);
            String mockJson = "{}";

            when(messageEventMapper.toMessageSentEvent(eq(message), any(UUID.class), eq(recipientId), eq(expectedSnippet)))
                    .thenReturn(mockKafkaEvent);
            when(kafkaTopicsProperties.messageEvents()).thenReturn(topicName);
            when(objectMapper.writeValueAsString(mockKafkaEvent)).thenReturn(mockJson);

            listener.handleMessageSavedEvent(event);

            verifyAndAssertOutboxEntitySaved(mockJson);
        }

        @Test
        void handleMessageSavedEvent_NullContent_HandlesGracefully() throws JsonProcessingException {
            Message message = createMessage(null);
            MessageSavedEvent event = new MessageSavedEvent(message, recipientId);
            MessageSentEvent mockKafkaEvent = createMockKafkaEvent(null);
            String mockJson = "{}";

            when(messageEventMapper.toMessageSentEvent(eq(message), any(UUID.class), eq(recipientId), eq(null)))
                    .thenReturn(mockKafkaEvent);
            when(kafkaTopicsProperties.messageEvents()).thenReturn(topicName);
            when(objectMapper.writeValueAsString(mockKafkaEvent)).thenReturn(mockJson);

            listener.handleMessageSavedEvent(event);

            verifyAndAssertOutboxEntitySaved(mockJson);
        }

        @Test
        void handleMessageSavedEvent_SerializationFails_ThrowsRuntimeException() throws JsonProcessingException {
            Message message = createMessage("Test");
            MessageSavedEvent event = new MessageSavedEvent(message, recipientId);
            MessageSentEvent mockKafkaEvent = createMockKafkaEvent("Test");

            when(messageEventMapper.toMessageSentEvent(eq(message), any(UUID.class), eq(recipientId), eq("Test")))
                    .thenReturn(mockKafkaEvent);
            when(objectMapper.writeValueAsString(mockKafkaEvent))
                    .thenThrow(new JsonProcessingException("Mock error") {
                    });

            assertThrows(RuntimeException.class, () ->
                    listener.handleMessageSavedEvent(event)
            );
        }
    }
}