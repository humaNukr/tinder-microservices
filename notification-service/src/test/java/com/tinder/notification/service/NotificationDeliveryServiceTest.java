package com.tinder.notification.service;

import com.tinder.notification.enums.DeviceType;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.provider.PushSender;
import com.tinder.notification.service.impl.NotificationDeliveryService;
import com.tinder.notification.service.interfaces.DeviceTokenService;
import com.tinder.notification.service.interfaces.NotificationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDeliveryService")
class NotificationDeliveryServiceTest {

    @Mock
    private NotificationLogService notificationLogService;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private PushSender pushSender;

    @InjectMocks
    private NotificationDeliveryService deliveryService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private com.tinder.notification.repository.projection.DeviceTokenInfo tokenInfo(String token) {
        return new com.tinder.notification.repository.projection.DeviceTokenInfo() {
            @Override
            public UUID getId() {
                return UUID.randomUUID();
            }

            @Override
            public UUID getUserId() {
                return userId;
            }

            @Override
            public String getDeviceType() {
                return DeviceType.ANDROID.name();
            }

            @Override
            public String getToken() {
                return token;
            }
        };
    }

    @Nested
    @DisplayName("deliver()")
    class Deliver {

        @Test
        @DisplayName("persists in-app notification before attempting push")
        void withTokens_PersistsBeforePush() {
            when(deviceTokenService.getUserTokens(userId)).thenReturn(List.of(tokenInfo("tok-1")));

            deliveryService.deliver(userId, "Title", "Body", NotificationType.MESSAGE, Map.of("k", "v"));

            InOrder order = inOrder(notificationLogService, pushSender);
            order.verify(notificationLogService).createNotification(
                    eq(userId), eq("Title"), eq("Body"), eq(NotificationType.MESSAGE), eq(Map.of("k", "v"))
            );
            order.verify(pushSender).sendNotification(eq("tok-1"), eq("Title"), eq("Body"), eq(Map.of("k", "v")));
        }

        @Test
        @DisplayName("skips push when user has no device tokens")
        void noTokens_SkipsPush() {
            when(deviceTokenService.getUserTokens(userId)).thenReturn(List.of());

            deliveryService.deliver(userId, "Title", "Body", NotificationType.MATCH, Map.of());

            verify(notificationLogService).createNotification(
                    eq(userId), eq("Title"), eq("Body"), eq(NotificationType.MATCH), eq(Map.of())
            );
            verify(pushSender, never()).sendNotification(anyString(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("keeps in-app notification when push throws")
        void pushThrows_InAppStillSaved() {
            when(deviceTokenService.getUserTokens(userId)).thenReturn(List.of(tokenInfo("bad-token")));
            org.mockito.Mockito.doThrow(new RuntimeException("FCM down"))
                    .when(pushSender).sendNotification(anyString(), anyString(), anyString(), anyMap());

            deliveryService.deliver(userId, "Title", "Body", NotificationType.MESSAGE, Map.of());

            verify(notificationLogService).createNotification(
                    eq(userId), eq("Title"), eq("Body"), eq(NotificationType.MESSAGE), eq(Map.of())
            );
        }

        @Test
        @DisplayName("sends push to every registered device token")
        void multipleTokens_PushAllDevices() {
            when(deviceTokenService.getUserTokens(userId)).thenReturn(List.of(
                    tokenInfo("tok-1"),
                    tokenInfo("tok-2")
            ));

            deliveryService.deliver(userId, "T", "B", NotificationType.MESSAGE, Map.of());

            verify(pushSender).sendNotification(eq("tok-1"), eq("T"), eq("B"), eq(Map.of()));
            verify(pushSender).sendNotification(eq("tok-2"), eq("T"), eq("B"), eq(Map.of()));
        }
    }
}
