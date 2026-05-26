package com.tinder.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.notification.event.MatchEvent;
import com.tinder.notification.processor.MatchNotificationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchEventListener")
class MatchEventListenerTest {

    @Mock
    private MatchNotificationProcessor matchNotificationProcessor;

    private MatchEventListener listener;
    private ObjectMapper objectMapper;
    private MatchEvent event;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new MatchEventListener(matchNotificationProcessor, objectMapper);
        event = new MatchEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private String toJson(MatchEvent value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    @Nested
    @DisplayName("handleMatchEvent()")
    class HandleMatchEvent {

        @Test
        @DisplayName("delegates valid payload to processor")
        void validPayload_ProcessesEvent() throws Exception {
            listener.handleMatchEvent(toJson(event));

            verify(matchNotificationProcessor).process(event.eventId(), event.user1Id(), event.user2Id());
        }

        @Test
        @DisplayName("propagates JsonProcessingException for malformed payload")
        void malformedPayload_ThrowsJsonProcessingException() {
            assertThrows(JsonProcessingException.class, () -> listener.handleMatchEvent("not-json"));
        }

        @Test
        @DisplayName("propagates runtime failures for Kafka retry")
        void processingFailure_PropagatesException() throws Exception {
            doThrow(new RuntimeException("database unavailable"))
                    .when(matchNotificationProcessor).process(event.eventId(), event.user1Id(), event.user2Id());

            assertThrows(RuntimeException.class, () -> listener.handleMatchEvent(toJson(event)));
        }
    }
}
