package com.tinder.chat.messaging.redis;

import com.tinder.chat.chat.Chat;
import com.tinder.chat.chat.ChatParticipantProvider;
import com.tinder.chat.chat.ChatRepository;
import com.tinder.chat.config.RedisChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatParticipantProvider implements ChatParticipantProvider {

    private final StringRedisTemplate redisTemplate;
    private final ChatRepository chatRepository;
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
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found with id " + chatId));

        saveParticipants(chat.getId(), chat.getUser1Id(), chat.getUser2Id());

        return Set.of(chat.getUser1Id(), chat.getUser2Id());
    }

    private String buildKey(UUID chatId) {
        return redisProps.keyPrefix() + chatId + redisProps.keySuffix();
    }
}