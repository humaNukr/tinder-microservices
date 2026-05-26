package com.tinder.notification.service;

import com.tinder.notification.dto.SaveTokenRequest;
import com.tinder.notification.entity.DeviceToken;
import com.tinder.notification.enums.DeviceType;
import com.tinder.notification.mapper.DeviceTokenMapper;
import com.tinder.notification.repository.DeviceTokenRepository;
import com.tinder.notification.service.impl.DeviceTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceTokenServiceImpl")
class DeviceTokenServiceImplTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private DeviceTokenMapper deviceTokenMapper;

    @InjectMocks
    private DeviceTokenServiceImpl deviceTokenService;

    private UUID userId;
    private SaveTokenRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        request = new SaveTokenRequest("fcm-token", DeviceType.ANDROID);
    }

    @Nested
    @DisplayName("saveDeviceToken()")
    class SaveDeviceToken {

        @Test
        @DisplayName("persists new token when not registered yet")
        void newToken_SavesEntity() {
            when(deviceTokenRepository.findByToken(request.token())).thenReturn(Optional.empty());
            DeviceToken mapped = DeviceToken.builder().token(request.token()).deviceType(DeviceType.ANDROID).build();
            when(deviceTokenMapper.toEntity(request)).thenReturn(mapped);

            deviceTokenService.saveDeviceToken(userId, request);

            verify(deviceTokenRepository).save(mapped);
        }

        @Test
        @DisplayName("reassigns userId when token already exists for another user")
        void existingTokenDifferentUser_UpdatesUserId() {
            UUID previousOwner = UUID.randomUUID();
            DeviceToken existing = DeviceToken.builder()
                    .userId(previousOwner)
                    .token(request.token())
                    .deviceType(DeviceType.IOS)
                    .build();
            when(deviceTokenRepository.findByToken(request.token())).thenReturn(Optional.of(existing));

            deviceTokenService.saveDeviceToken(userId, request);

            assertEquals(userId, existing.getUserId());
            verify(deviceTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("does nothing when same user re-registers the same token")
        void sameUserSameToken_NoSave() {
            DeviceToken existing = DeviceToken.builder()
                    .userId(userId)
                    .token(request.token())
                    .deviceType(DeviceType.ANDROID)
                    .build();
            when(deviceTokenRepository.findByToken(request.token())).thenReturn(Optional.of(existing));

            deviceTokenService.saveDeviceToken(userId, request);

            verify(deviceTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteToken()")
    class DeleteToken {

        @Test
        @DisplayName("deletes token from repository when value is present")
        void validToken_DeletesByToken() {
            deviceTokenService.deleteToken("stale-token");

            verify(deviceTokenRepository).deleteByToken("stale-token");
        }

        @Test
        @DisplayName("ignores null or blank tokens")
        void blankToken_NoRepositoryCall() {
            deviceTokenService.deleteToken(null);
            deviceTokenService.deleteToken("  ");

            verify(deviceTokenRepository, never()).deleteByToken(any());
        }
    }
}
