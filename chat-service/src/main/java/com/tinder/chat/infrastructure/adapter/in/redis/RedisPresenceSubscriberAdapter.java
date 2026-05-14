package com.tinder.chat.infrastructure.adapter.in.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.out.notification.ClientNotificationPort;
import com.tinder.chat.shared.dto.event.UserPresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPresenceSubscriberAdapter {

    private final ObjectMapper objectMapper;
    private final ClientNotificationPort notificationPort;

    public void handlePresenceEvent(String messageBody) throws JsonProcessingException {
        UserPresenceEvent event = objectMapper.readValue(messageBody, UserPresenceEvent.class);
        log.info("Broadcasting presence for user: {}", event.userId());
        notificationPort.sendPresenceEvent(event);
    }
}