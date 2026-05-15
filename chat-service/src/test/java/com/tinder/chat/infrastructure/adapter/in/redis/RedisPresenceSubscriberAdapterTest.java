package com.tinder.chat.infrastructure.adapter.in.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.out.notification.ClientNotificationPort;
import com.tinder.chat.shared.dto.event.UserPresenceEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPresenceSubscriberAdapterTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ClientNotificationPort notificationPort;

    @InjectMocks
    private RedisPresenceSubscriberAdapter adapter;

    @Nested
    class HandlePresenceEvent {

        @Test
        void handlePresenceEvent_ValidJson_ParsesAndSendsNotification() throws JsonProcessingException {
            String json = "{\"userId\":\"123e4567-e89b-12d3-a456-426614174000\",\"online\":true}";
            UserPresenceEvent mockEvent = mock(UserPresenceEvent.class);

            when(objectMapper.readValue(json, UserPresenceEvent.class)).thenReturn(mockEvent);

            adapter.handlePresenceEvent(json);

            verify(objectMapper).readValue(json, UserPresenceEvent.class);
            verify(notificationPort).sendPresenceEvent(mockEvent);
        }
    }
}