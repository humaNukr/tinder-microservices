package com.tinder.auth.service;

import com.tinder.auth.properties.JwtProperties;
import com.tinder.auth.properties.RedisAuthProperties;
import com.tinder.auth.service.impl.RedisTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTokenServiceImplTest {

    private final UUID userId = UUID.randomUUID();
    private final String deviceId = "device-123";
    private final String token = "refresh.token.string";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private RedisAuthProperties redisAuthProperties;

    @InjectMocks
    private RedisTokenServiceImpl tokenService;

    private String expectedKey;

    @BeforeEach
    void setUp() {
        lenient().when(redisAuthProperties.sessionPrefix()).thenReturn("session:");
        lenient().when(redisAuthProperties.sessionSuffix()).thenReturn(":tokens");
        expectedKey = "session:" + userId + ":tokens";
    }

    @Test
    @DisplayName("Should store token and set expiration atomically via SessionCallback")
    @SuppressWarnings("unchecked")
    void storeRefreshToken_ValidData_StoresInHashAndSetsExpirationAtomically() {
        when(jwtProperties.refreshTokenExpirationMs()).thenReturn(86400000L);

        RedisOperations<String, String> operationsMock = mock(RedisOperations.class);
        when(operationsMock.opsForHash()).thenReturn(hashOperations);

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<List<Object>> callback = invocation.getArgument(0);
            return callback.execute(operationsMock);
        });

        tokenService.storeRefreshToken(userId, deviceId, token);

        verify(operationsMock).multi();
        verify(hashOperations).put(expectedKey, deviceId, token);
        verify(operationsMock).expire(expectedKey, Duration.ofMillis(86400000L));
        verify(operationsMock).exec();
    }

    @Test
    void getRefreshToken_TokenExists_ReturnsToken() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(expectedKey, deviceId)).thenReturn(token);

        String result = tokenService.getRefreshToken(userId, deviceId);

        assertEquals(token, result);
    }

    @Test
    void getRefreshToken_TokenDoesNotExist_ReturnsNull() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(expectedKey, deviceId)).thenReturn(null);

        String result = tokenService.getRefreshToken(userId, deviceId);

        assertNull(result);
    }

    @Test
    void deleteRefreshToken_ValidData_DeletesFromHash() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        tokenService.deleteRefreshToken(userId, deviceId);

        verify(hashOperations).delete(expectedKey, deviceId);
    }

    @Test
    void deleteAllUserTokens_ValidUserId_DeletesKey() {
        tokenService.deleteAllUserTokens(userId);

        verify(redisTemplate).delete(expectedKey);
    }
}