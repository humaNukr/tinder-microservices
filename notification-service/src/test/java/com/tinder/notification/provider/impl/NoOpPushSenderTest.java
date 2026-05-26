package com.tinder.notification.provider.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("NoOpPushSender")
class NoOpPushSenderTest {

    private final NoOpPushSender noOpPushSender = new NoOpPushSender();

    @Nested
    @DisplayName("sendNotification()")
    class SendNotification {

        @Test
        @DisplayName("completes without error and ignores payload")
        void anyPayload_DoesNotThrow() {
            assertDoesNotThrow(() -> noOpPushSender.sendNotification(
                    "token",
                    "Title",
                    "Body",
                    Map.of("key", "value")
            ));
        }

        @Test
        @DisplayName("accepts empty data map")
        void emptyData_DoesNotThrow() {
            assertDoesNotThrow(() -> noOpPushSender.sendNotification(
                    "token",
                    "Title",
                    "Body",
                    Map.of()
            ));
        }
    }
}
