package com.tinder.chat.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.config.WebSocketProperties;
import com.tinder.chat.infrastructure.kafka.contract.UserPresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPresenceHandlers {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    public void handlePresenceEvent(String messageBody) throws JsonProcessingException {
        UserPresenceEvent event = objectMapper.readValue(messageBody, UserPresenceEvent.class);
        log.info("Broadcasting presence for user: {}", event.userId());

        messagingTemplate.convertAndSend(
                webSocketProperties.topicUsersPrefix() + event.userId()
                        + webSocketProperties.topicUsersPresenceSuffix(),
                event
        );
    }
}