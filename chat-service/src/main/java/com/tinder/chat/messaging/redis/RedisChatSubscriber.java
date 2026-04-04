package com.tinder.chat.messaging.redis;

import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.messaging.ChatEventSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.tinder.chat.config.WebSocketConstants.QUEUE_MESSAGES;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements ChatEventSubscriber {
    private final SimpMessagingTemplate messagingTemplate;

    public void handleMessage(MessageEventDto eventDto) {
        log.info(
                "Received message from Redis: chat={}, sender={}, recipient={}",
                eventDto.chatId(), eventDto.senderId(), eventDto.recipientId()
        );

        messagingTemplate.convertAndSendToUser(
                eventDto.recipientId().toString(),
                QUEUE_MESSAGES,
                eventDto
        );
    }
}