package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.application.port.out.room.ChatPersistencePort;
import com.tinder.chat.infrastructure.config.properties.RedisChatProperties;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisChatParticipantAdapterIT extends IntegrationTestBase {

    @Autowired
    private RedisChatParticipantAdapter adapter;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired // Використовуємо реальний бін замість моку
    private RedisChatProperties redisProps;

    @MockitoBean // Це залишається моком, бо ми не хочемо лізти в БД у цьому тесті
    private ChatPersistencePort chatPersistencePort;

    private UUID chatId;
    private String redisKey;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();

        // Будуємо ключ, використовуючи реальні проперті з тестового контексту
        redisKey = redisProps.keyPrefix() + chatId + redisProps.keySuffix();

        // Очищаємо кеш перед кожним тестом
        redisTemplate.delete(redisKey);
    }

    @Nested
    class GetParticipants {

        @Test
        void getParticipants_InCache_ReturnsFromRedis() {
            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.randomUUID();
            redisTemplate.opsForSet().add(redisKey, u1.toString(), u2.toString());

            Set<UUID> result = adapter.getParticipants(chatId);

            assertEquals(2, result.size());
            assertTrue(result.contains(u1));
            assertTrue(result.contains(u2));
        }

        @Test
        void getParticipants_CacheMiss_FetchesFromDbAndUpdatesCache() {
            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.randomUUID();
            when(chatPersistencePort.getChatParticipants(chatId)).thenReturn(Set.of(u1, u2));

            Set<UUID> result = adapter.getParticipants(chatId);

            assertEquals(2, result.size());
            verify(chatPersistencePort).getChatParticipants(chatId);

            Set<String> cached = redisTemplate.opsForSet().members(redisKey);
            assertNotNull(cached);
            assertEquals(2, cached.size());
        }
    }
}