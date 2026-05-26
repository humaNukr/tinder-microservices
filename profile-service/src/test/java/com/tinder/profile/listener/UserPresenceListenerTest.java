package com.tinder.profile.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.profile.event.UserPresenceEvent;
import com.tinder.profile.service.interfaces.ProfileService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPresenceListener")
class UserPresenceListenerTest {

    @Mock
    private ProfileService profileService;

    private UserPresenceListener listener;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new UserPresenceListener(profileService, objectMapper);
    }

    private String toJson(UserPresenceEvent event) throws Exception {
        return objectMapper.writeValueAsString(event);
    }

    @Nested
    @DisplayName("handlePresenceEvent()")
    class HandlePresence {

        @Test
        @DisplayName("updates last seen from event")
        void validEvent_UpdatesLastSeen() throws Exception {
            UUID userId = UUID.randomUUID();
            Instant ts = Instant.parse("2026-01-10T12:00:00Z");
            UserPresenceEvent event = new UserPresenceEvent(userId, true, ts);

            listener.handlePresenceEvent(toJson(event));

            verify(profileService).updateLastSeen(userId, ts);
        }

        @Test
        @DisplayName("propagates failures for Kafka retry")
        void serviceFailure_Propagates() throws Exception {
            UUID userId = UUID.randomUUID();
            UserPresenceEvent event = new UserPresenceEvent(userId, false, Instant.now());
            doThrow(new RuntimeException("db")).when(profileService).updateLastSeen(userId, event.timestamp());

            assertThrows(RuntimeException.class, () -> listener.handlePresenceEvent(toJson(event)));
        }
    }
}
