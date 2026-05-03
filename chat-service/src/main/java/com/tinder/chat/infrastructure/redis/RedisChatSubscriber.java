package com.tinder.chat.infrastructure.redis;

import com.tinder.chat.chat.port.ChatEventSubscriber;
import com.tinder.chat.config.WebSocketProperties;
import com.tinder.chat.message.dto.MessageEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements ChatEventSubscriber {
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    public void handleMessage(MessageEventDto eventDto) {
        log.info(
                "Received message from Redis: chat={}, sender={}, recipient={}",
                eventDto.chatId(), eventDto.senderId(), eventDto.recipientId()
        );

        messagingTemplate.convertAndSendToUser(
                eventDto.recipientId().toString(),
                webSocketProperties.queueMessages(),
                eventDto
        );
    }
}