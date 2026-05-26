package com.tinder.notification.provider.impl;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.tinder.notification.service.interfaces.DeviceTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmPushSender")
class FcmPushSenderTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private DeviceTokenService deviceTokenService;

    @InjectMocks
    private FcmPushSender fcmPushSender;

    private String token;
    private Map<String, String> data;

    @BeforeEach
    void setUp() {
        token = "fcm-device-token";
        data = Map.of("eventId", "abc-123");
    }

    @Nested
    @DisplayName("sendNotification()")
    class SendNotification {

        @Test
        @DisplayName("sends message via Firebase")
        void success_SendsViaFirebase() throws Exception {
            when(firebaseMessaging.send(any(Message.class))).thenReturn("projects/test/messages/1");

            fcmPushSender.sendNotification(token, "Title", "Body", data);

            verify(firebaseMessaging).send(any(Message.class));
            verify(deviceTokenService, never()).deleteToken(any());
        }

        @Test
        @DisplayName("deletes token when FCM reports UNREGISTERED")
        void unregisteredToken_DeletesFromDatabase() throws Exception {
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

            fcmPushSender.sendNotification(token, "Title", "Body", data);

            verify(deviceTokenService).deleteToken(token);
        }

        @Test
        @DisplayName("deletes token when FCM reports INVALID_ARGUMENT via messaging error code")
        void invalidArgumentMessagingCode_DeletesFromDatabase() throws Exception {
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

            fcmPushSender.sendNotification(token, "Title", "Body", data);

            verify(deviceTokenService).deleteToken(token);
        }

        @Test
        @DisplayName("deletes token when base error code is INVALID_ARGUMENT")
        void invalidArgumentBaseCode_DeletesFromDatabase() throws Exception {
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            when(exception.getMessagingErrorCode()).thenReturn(null);
            when(exception.getErrorCode()).thenReturn(ErrorCode.INVALID_ARGUMENT);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

            fcmPushSender.sendNotification(token, "Title", "Body", data);

            verify(deviceTokenService).deleteToken(token);
        }

        @Test
        @DisplayName("keeps token when FCM fails with unexpected error")
        void unexpectedError_DoesNotDeleteToken() throws Exception {
            FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
            when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
            when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);

            fcmPushSender.sendNotification(token, "Title", "Body", data);

            verify(deviceTokenService, never()).deleteToken(any());
        }
    }
}
