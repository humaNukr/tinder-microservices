package com.tinder.notification.provider.impl;

import com.tinder.notification.provider.PushSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnMissingBean(PushSender.class)
public class NoOpPushSender implements PushSender {

    @Override
    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        log.debug("Push skipped: Firebase is not configured");
    }
}
