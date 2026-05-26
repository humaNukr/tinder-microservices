package com.tinder.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.notification.event.MessageSentEvent;
import com.tinder.notification.processor.MessageNotificationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageEventListener")
class MessageEventListenerTest {

    @Mock
    private MessageNotificationProcessor notificationFacade;

    private MessageEventListener listener;
    private ObjectMapper objectMapper;
    private MessageSentEvent event;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new MessageEventListener(notificationFacade, objectMapper);
        event = new MessageSentEvent(
                UUID.randomUUID(),
                1L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TEXT",
                "Hi",
                Instant.now()
        );
    }

    private String toJson(MessageSentEvent value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    @Nested
    @DisplayName("handleMessageEvent()")
    class HandleMessageEvent {

        @Test
        @DisplayName("delegates valid payload to processor")
        void validPayload_ProcessesEvent() throws Exception {
            listener.handleMessageEvent(toJson(event));

            verify(notificationFacade).process(event);
        }

        @Test
        @DisplayName("propagates JsonProcessingException for malformed payload")
        void malformedPayload_ThrowsJsonProcessingException() {
            assertThrows(JsonProcessingException.class, () -> listener.handleMessageEvent("{invalid"));
        }

        @Test
        @DisplayName("propagates runtime failures for Kafka retry")
        void processingFailure_PropagatesException() throws Exception {
            doThrow(new RuntimeException("database unavailable"))
                    .when(notificationFacade).process(any());

            assertThrows(RuntimeException.class, () -> listener.handleMessageEvent(toJson(event)));
        }
    }
}
