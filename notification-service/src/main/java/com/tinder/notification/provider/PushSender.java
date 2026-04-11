package com.tinder.notification.provider;

import java.util.Map;

public interface PushSender {
    void sendNotification(String token, String title, String body, Map<String, String> data);
}