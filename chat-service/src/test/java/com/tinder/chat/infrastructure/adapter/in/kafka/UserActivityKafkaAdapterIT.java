package com.tinder.chat.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.chat.domain.enums.ActivityType;
import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.enums.MessageStatus;
import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.infrastructure.adapter.out.persistence.ChatPersistenceAdapter;
import com.tinder.chat.infrastructure.adapter.out.persistence.entity.MessageJpaEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.AccountDeletionJpaRepository;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.MessageJpaRepository;
import com.tinder.chat.infrastructure.config.properties.RedisChatProperties;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import com.tinder.chat.shared.dto.event.UserActivityEvent;
import com.tinder.chat.util.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UserActivityKafkaAdapter — Integration Tests")
class UserActivityKafkaAdapterIT extends IntegrationTestBase {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatPersistenceAdapter chatPersistenceAdapter;

    @Autowired
    private MessageJpaRepository messageJpaRepository;

    @Autowired
    private AccountDeletionJpaRepository accountDeletionJpaRepository;

    @Autowired
    private InboxEventJpaRepository inboxEventRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisChatProperties redisChatProperties;

    @Autowired
    private RedisPresenceProperties redisPresenceProperties;

    @Value("${app.kafka.topics.user-activity}")
    private String topic;

    @BeforeEach
    void setUp() {
        messageJpaRepository.deleteAll();
        accountDeletionJpaRepository.deleteAll();
        inboxEventRepository.deleteAll();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("DELETE_ACCOUNT removes chats, messages and Redis keys")
    void deleteAccount_RemovesData() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        boolean userIsSmaller = userId.compareTo(partnerId) < 0;
        UUID user1 = userIsSmaller ? userId : partnerId;
        UUID user2 = userIsSmaller ? partnerId : userId;

        UUID chatId = UUID.randomUUID();
        chatPersistenceAdapter.save(Chat.builder()
                .id(chatId)
                .user1Id(user1)
                .user2Id(user2)
                .build());

        MessageJpaEntity message = MessageJpaEntity.builder()
                .chatId(chatId)
                .senderId(userId)
                .contentType(MessageContentType.TEXT)
                .content("hello")
                .status(MessageStatus.SENT)
                .build();
        messageJpaRepository.save(message);

        String participantsKey = redisChatProperties.keyPrefix() + chatId + redisChatProperties.keySuffix();
        redisTemplate.opsForSet().add(participantsKey, userId.toString(), partnerId.toString());
        redisTemplate.opsForSet().add(redisPresenceProperties.sessionKeyPrefix() + userId, "session-1");

        UUID eventId = UUID.randomUUID();
        UserActivityEvent event =
                new UserActivityEvent(eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());
        kafkaTemplate.send(topic, userId.toString(), objectMapper.writeValueAsString(event)).get();

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    assertEquals(0, accountDeletionJpaRepository.count());
                    assertEquals(0, messageJpaRepository.count());
                });

        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(participantsKey)));
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(redisPresenceProperties.sessionKeyPrefix() + userId)));
        assertTrue(inboxEventRepository.existsByEventId(eventId));
    }
}
