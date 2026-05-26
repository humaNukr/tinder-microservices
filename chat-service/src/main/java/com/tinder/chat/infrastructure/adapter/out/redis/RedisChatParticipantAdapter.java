package com.tinder.chat.infrastructure.adapter.out.redis;

import com.tinder.chat.application.port.out.room.ChatParticipantPort;
import com.tinder.chat.application.port.out.room.ChatPersistencePort;
import com.tinder.chat.infrastructure.config.properties.RedisChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatParticipantAdapter implements ChatParticipantPort {

    private final StringRedisTemplate redisTemplate;
    private final ChatPersistencePort chatPersistencePort;
    private final RedisChatProperties redisProps;

    public void saveParticipants(UUID chatId, UUID user1Id, UUID user2Id) {
        String key = buildKey(chatId);
        redisTemplate.opsForSet().add(key, user1Id.toString(), user2Id.toString());
        redisTemplate.expire(key, redisProps.ttl());
    }

    public Set<UUID> getParticipants(UUID chatId) {
        String key = buildKey(chatId);
        Set<String> members = redisTemplate.opsForSet().members(key);

        if (members != null && !members.isEmpty()) {
            redisTemplate.expire(key, redisProps.ttl());
            return members.stream().map(UUID::fromString).collect(Collectors.toSet());
        }

        // Fallback
        log.warn("Cache miss for chat {}. Fallback to PostgreSQL", chatId);

        Set<UUID> participants = chatPersistencePort.getChatParticipants(chatId);
        List<UUID> participantList = new ArrayList<>(participants);
        if (participantList.size() != 2) {
            log.warn(
                    "Expected 2 participants for chat {}, found {}. Returning without caching.",
                    chatId,
                    participantList.size());
            return participants;
        }

        UUID user1Id = participantList.get(0);
        UUID user2Id = participantList.get(1);
        saveParticipants(chatId, user1Id, user2Id);

        return Set.of(user1Id, user2Id);
    }

    private String buildKey(UUID chatId) {
        return redisProps.keyPrefix() + chatId + redisProps.keySuffix();
    }
}