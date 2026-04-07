package com.tinder.notification.provider.impl;

import com.tinder.notification.provider.PushSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingPushSender implements PushSender {

    @Override
    public void sendNotification(String token, String title, String body) {
        log.info("-> PUSH SENT [Token: {}] | Title: '{}' | Body: '{}'", token, title, body);
    }
}