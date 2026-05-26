package com.tinder.chat.application.service.account;

import com.tinder.chat.application.port.out.media.MediaStoragePort;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.AccountDeletionJpaRepository;
import com.tinder.chat.infrastructure.config.properties.RedisChatProperties;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final AccountDeletionJpaRepository accountDeletionJpaRepository;
    private final MediaStoragePort mediaStoragePort;
    private final StringRedisTemplate redisTemplate;
    private final RedisChatProperties redisChatProperties;
    private final RedisPresenceProperties redisPresenceProperties;

    @Transactional
    public void deleteUserData(UUID userId) {
        List<UUID> chatIds = accountDeletionJpaRepository.findChatIdsByUserId(userId);
        List<String> mediaKeys = accountDeletionJpaRepository.findMediaObjectKeysByUserId(userId);

        mediaStoragePort.deleteObjects(mediaKeys);
        evictRedisState(userId, chatIds);

        int messagesDeleted = accountDeletionJpaRepository.deleteMessagesByUserId(userId);
        int chatsDeleted = accountDeletionJpaRepository.deleteChatsByUserId(userId);

        log.info(
                "Deleted {} messages, {} chats and {} media file(s) for user {} ({} Redis chat keys cleared)",
                messagesDeleted,
                chatsDeleted,
                mediaKeys.size(),
                userId,
                chatIds.size());
    }

    private void evictRedisState(UUID userId, List<UUID> chatIds) {
        for (UUID chatId : chatIds) {
            redisTemplate.delete(redisChatProperties.keyPrefix() + chatId + redisChatProperties.keySuffix());
        }
        redisTemplate.delete(redisPresenceProperties.sessionKeyPrefix() + userId);
        redisTemplate.delete(redisPresenceProperties.gracePeriodPrefix() + userId);
        redisTemplate.delete("user:" + userId + ":profile");
    }
}
