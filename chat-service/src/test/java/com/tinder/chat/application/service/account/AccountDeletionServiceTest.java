package com.tinder.chat.application.service.account;

import com.tinder.chat.application.port.out.media.MediaStoragePort;
import com.tinder.chat.infrastructure.adapter.out.persistence.repository.AccountDeletionJpaRepository;
import com.tinder.chat.infrastructure.config.properties.RedisChatProperties;
import com.tinder.chat.infrastructure.config.properties.RedisPresenceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountDeletionService")
class AccountDeletionServiceTest {

    @Mock
    private AccountDeletionJpaRepository accountDeletionJpaRepository;

    @Mock
    private MediaStoragePort mediaStoragePort;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisChatProperties redisChatProperties;

    @Mock
    private RedisPresenceProperties redisPresenceProperties;

    @InjectMocks
    private AccountDeletionService accountDeletionService;

    @Test
    @DisplayName("deleteUserData removes media from MinIO before database rows")
    void deleteUserData_DeletesMediaObjects() {
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        List<String> mediaKeys = List.of("chats/" + chatId + "/file1.jpg", "chats/" + chatId + "/file2.mp4");

        when(accountDeletionJpaRepository.findChatIdsByUserId(userId)).thenReturn(List.of(chatId));
        when(accountDeletionJpaRepository.findMediaObjectKeysByUserId(userId)).thenReturn(mediaKeys);
        when(redisChatProperties.keyPrefix()).thenReturn("chat:");
        when(redisChatProperties.keySuffix()).thenReturn(":participants");
        when(redisPresenceProperties.sessionKeyPrefix()).thenReturn("user:sessions:");
        when(redisPresenceProperties.gracePeriodPrefix()).thenReturn("user:grace:");

        accountDeletionService.deleteUserData(userId);

        verify(mediaStoragePort).deleteObjects(mediaKeys);
        verify(accountDeletionJpaRepository).deleteMessagesByUserId(userId);
        verify(accountDeletionJpaRepository).deleteChatsByUserId(userId);
    }
}
