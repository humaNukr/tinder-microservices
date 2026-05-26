package com.tinder.profile.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.profile.event.ActivityType;
import com.tinder.profile.event.UserActivityEvent;
import com.tinder.profile.processor.UserActivityProcessor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserActivityListener")
class UserActivityListenerTest {

    @Mock
    private UserActivityProcessor userActivityProcessor;

    private UserActivityListener listener;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        listener = new UserActivityListener(userActivityProcessor, objectMapper);
    }

    private String toJson(UserActivityEvent event) throws Exception {
        return objectMapper.writeValueAsString(event);
    }

    @Nested
    @DisplayName("handleUserActivity()")
    class HandleUserActivity {

        @Test
        @DisplayName("delegates DELETE_ACCOUNT to processor")
        void deleteAccount_ProcessesEvent() throws Exception {
            UserActivityEvent event = new UserActivityEvent(
                    UUID.randomUUID(), UUID.randomUUID(), ActivityType.DELETE_ACCOUNT, Instant.now());

            listener.handleUserActivity(toJson(event));

            verify(userActivityProcessor).deleteProfileData(event);
        }

        @Test
        @DisplayName("ignores non-delete activity types")
        void otherType_SkipsProcessor() throws Exception {
            UserActivityEvent event = new UserActivityEvent(
                    UUID.randomUUID(), UUID.randomUUID(), ActivityType.LOGIN, Instant.now());

            listener.handleUserActivity(toJson(event));

            verify(userActivityProcessor, never()).deleteProfileData(any());
        }

        @Test
        @DisplayName("propagates processing failures for Kafka retry")
        void processingFailure_PropagatesException() throws Exception {
            UserActivityEvent event = new UserActivityEvent(
                    UUID.randomUUID(), UUID.randomUUID(), ActivityType.DELETE_ACCOUNT, Instant.now());
            doThrow(new RuntimeException("db down"))
                    .when(userActivityProcessor).deleteProfileData(event);

            assertThrows(RuntimeException.class, () -> listener.handleUserActivity(toJson(event)));
        }
    }
}
