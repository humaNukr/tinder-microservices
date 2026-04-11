package com.tinder.notification.provider.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.tinder.notification.provider.PushSender;
import com.tinder.notification.service.interfaces.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushSender implements PushSender {

    private final DeviceTokenService deviceTokenService;

    @Override
    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Push notification sent successfully. ID: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Error sending push to token: {}", token);

            MessagingErrorCode msgErrorCode = e.getMessagingErrorCode();
            String baseErrorCode = (e.getErrorCode() != null) ? e.getErrorCode().name() : "";

            boolean isUnregistered = (msgErrorCode == MessagingErrorCode.UNREGISTERED);
            boolean isMsgInvalidArgument = (msgErrorCode == MessagingErrorCode.INVALID_ARGUMENT);
            boolean isBaseInvalidArgument = "INVALID_ARGUMENT".equals(baseErrorCode);

            if (isUnregistered || isMsgInvalidArgument || isBaseInvalidArgument) {
                log.warn("Token is invalid/unregistered. Deleting from DB: {}", token);
                deviceTokenService.deleteToken(token);
            } else {
                log.error("Unexpected Firebase error: {}", e.getMessage());
            }
        }
    }
}
