package com.tinder.chat.infrastructure.adapter.in.redis;

import com.tinder.chat.application.port.in.presence.UpdatePresenceUseCase;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceGracePeriodRedisAdapterTest {

    private final String gracePrefix = "presence:grace:";
    @Mock
    private UpdatePresenceUseCase updatePresenceUseCase;
    @Mock
    private RedisPresenceProperties presenceProperties;
    @Mock
    private RedisMessageListenerContainer listenerContainer;
    @InjectMocks
    private PresenceGracePeriodRedisAdapter adapter;

    @BeforeEach
    void setUp() {
        when(presenceProperties.gracePeriodPrefix()).thenReturn(gracePrefix);
    }

    @Nested
    class OnMessage {

        @Test
        void onMessage_ValidGraceKey_CallsUseCase() {
            UUID userId = UUID.randomUUID();
            String expiredKey = gracePrefix + userId;
            Message message = mock(Message.class);
            when(message.toString()).thenReturn(expiredKey);

            adapter.onMessage(message, null);

            verify(updatePresenceUseCase).handleGracePeriodExpired(userId);
        }

        @Test
        void onMessage_DifferentPrefix_IgnoresEvent() {
            String expiredKey = "some:other:key:123";
            Message message = mock(Message.class);
            when(message.toString()).thenReturn(expiredKey);

            adapter.onMessage(message, null);

            verifyNoInteractions(updatePresenceUseCase);
        }

        @Test
        void onMessage_InvalidUuid_CatchesExceptionAndIgnores() {
            String expiredKey = gracePrefix + "invalid-uuid-string";
            Message message = mock(Message.class);
            when(message.toString()).thenReturn(expiredKey);

            adapter.onMessage(message, null);

            verifyNoInteractions(updatePresenceUseCase);
        }
    }
}