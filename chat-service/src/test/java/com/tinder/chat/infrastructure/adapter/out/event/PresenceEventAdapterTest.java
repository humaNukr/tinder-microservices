package com.tinder.chat.infrastructure.adapter.out.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.infrastructure.config.properties.KafkaTopicsProperties;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import com.tinder.chat.shared.dto.event.UserPresenceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresenceEventAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private RedisPresenceProperties redisPresenceProperties;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private KafkaTopicsProperties topicsProperties;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PresenceEventAdapter adapter;

    private UserPresenceEvent event;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        event = new UserPresenceEvent(userId, true, LocalDateTime.now());
    }

    @Nested
    class BroadcastToChatServers {

        @Test
        void broadcastToChatServers_ValidEvent_SendsToRedis() {
            String channel = "presence-channel";
            when(redisPresenceProperties.channel()).thenReturn(channel);

            adapter.broadcastToChatServers(event);

            verify(redisTemplate).convertAndSend(channel, event);
        }

        @Test
        void broadcastToChatServers_RedisThrowsException_CatchesGracefully() {
            String channel = "presence-channel";
            when(redisPresenceProperties.channel()).thenReturn(channel);
            doThrow(new RuntimeException("Redis down")).when(redisTemplate).convertAndSend(channel, event);

            assertDoesNotThrow(() -> adapter.broadcastToChatServers(event));
        }
    }

    @Nested
    class BroadcastToSystem {

        @Test
        void broadcastToSystem_ValidEvent_SendsToKafka() throws JsonProcessingException {
            String topic = "presence-topic";
            String jsonPayload = "{\"userId\":\"" + userId + "\"}";
            when(topicsProperties.userPresenceEvents()).thenReturn(topic);
            when(objectMapper.writeValueAsString(event)).thenReturn(jsonPayload);

            adapter.broadcastToSystem(event);

            verify(kafkaTemplate).send(topic, userId.toString(), jsonPayload);
        }

        @Test
        void broadcastToSystem_SerializationFails_CatchesGracefully() throws JsonProcessingException {
            when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("Mock error") {
            });

            assertDoesNotThrow(() -> adapter.broadcastToSystem(event));
        }

        @Test
        void broadcastToSystem_KafkaThrowsException_CatchesGracefully() throws JsonProcessingException {
            String topic = "presence-topic";
            String jsonPayload = "{\"userId\":\"" + userId + "\"}";
            when(topicsProperties.userPresenceEvents()).thenReturn(topic);
            when(objectMapper.writeValueAsString(event)).thenReturn(jsonPayload);
            when(kafkaTemplate.send(any(), any(), any())).thenThrow(new RuntimeException("Kafka down"));

            assertDoesNotThrow(() -> adapter.broadcastToSystem(event));
        }
    }
}