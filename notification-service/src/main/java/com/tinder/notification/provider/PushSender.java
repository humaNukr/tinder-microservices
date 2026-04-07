package com.tinder.notification.provider;

public interface PushSender {
    void sendNotification(String token, String title, String body);
}