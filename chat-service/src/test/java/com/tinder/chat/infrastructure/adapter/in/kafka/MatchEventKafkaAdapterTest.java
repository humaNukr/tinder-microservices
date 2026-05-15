package com.tinder.chat.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.in.room.CreateChatUseCase;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import com.tinder.chat.shared.dto.event.MatchEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchEventKafkaAdapterTest {

    @Mock
    private InboxEventJpaRepository inboxEventRepository;
    @Mock
    private CreateChatUseCase createChatUseCase;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private MatchEventKafkaAdapter matchEventKafkaAdapter;

    @Captor
    private ArgumentCaptor<InboxEventEntity> entityCaptor;

    private String validJson;
    private MatchEvent matchEvent;
    private UUID eventId;
    private UUID user1Id;
    private UUID user2Id;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        validJson = String.format("{\"eventId\":\"%s\",\"user1Id\":\"%s\",\"user2Id\":\"%s\"}", eventId, user1Id, user2Id);
        matchEvent = new MatchEvent(eventId, user1Id, user2Id);
    }

    private void setupTransactionTemplateMock() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Nested
    class ListenAndProcessMatchEvent {

        @Test
        void listenAndProcessMatchEvent_ValidNewEvent_CreatesChatAndSavesInboxEvent() throws Exception {
            when(objectMapper.readValue(validJson, MatchEvent.class)).thenReturn(matchEvent);
            setupTransactionTemplateMock();
            when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);

            matchEventKafkaAdapter.listenAndProcessMatchEvent(validJson);

            verify(inboxEventRepository).existsByEventId(eventId);
            verify(inboxEventRepository).save(entityCaptor.capture());
            assertEquals(eventId, entityCaptor.getValue().getEventId());
            verify(createChatUseCase).createChat(user1Id, user2Id);
        }

        @Test
        void listenAndProcessMatchEvent_DuplicateEvent_SkipsProcessing() throws Exception {
            when(objectMapper.readValue(validJson, MatchEvent.class)).thenReturn(matchEvent);
            setupTransactionTemplateMock();
            when(inboxEventRepository.existsByEventId(eventId)).thenReturn(true);

            matchEventKafkaAdapter.listenAndProcessMatchEvent(validJson);

            verify(inboxEventRepository).existsByEventId(eventId);
            verify(inboxEventRepository, never()).save(any());
            verifyNoInteractions(createChatUseCase);
        }

        @Test
        void listenAndProcessMatchEvent_InvalidJson_LogsErrorAndSkipsTransaction() throws Exception {
            String invalidJson = "invalid";
            when(objectMapper.readValue(invalidJson, MatchEvent.class)).thenThrow(JsonProcessingException.class);

            matchEventKafkaAdapter.listenAndProcessMatchEvent(invalidJson);

            verifyNoInteractions(transactionTemplate, inboxEventRepository, createChatUseCase);
        }

        @Test
        void listenAndProcessMatchEvent_DatabaseError_ThrowsException() throws Exception {
            when(objectMapper.readValue(validJson, MatchEvent.class)).thenReturn(matchEvent);
            doThrow(new RuntimeException("DB Connection failed"))
                    .when(transactionTemplate).executeWithoutResult(any());

            assertThrows(RuntimeException.class, () ->
                    matchEventKafkaAdapter.listenAndProcessMatchEvent(validJson)
            );
        }
    }
}