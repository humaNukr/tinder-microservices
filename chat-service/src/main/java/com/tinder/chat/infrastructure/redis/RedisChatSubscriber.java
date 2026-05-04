package com.tinder.chat.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.config.RedisChatProperties;
import com.tinder.chat.config.WebSocketProperties;
import com.tinder.chat.message.dto.MessageEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;
    private final RedisChatProperties redisProperties;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            if (channel.equals(redisProperties.channel())) {

                MessageEventDto eventDto = objectMapper.readValue(body, MessageEventDto.class);
                handleChatMessage(eventDto);

            } else if (channel.equals(redisProperties.typingChannel())) {

                TypingEventDto typingDto = objectMapper.readValue(body, TypingEventDto.class);
                handleTypingEvent(typingDto);

            } else {
                log.warn("Received message from unknown Redis channel: {}", channel);
            }
        } catch (Exception e) {
            log.error("Failed to process message from Redis channel: {}", channel, e);
        }
    }

    private void handleChatMessage(MessageEventDto eventDto) {
        log.info("Processing chat message for recipient={}", eventDto.recipientId());

        messagingTemplate.convertAndSendToUser(
                eventDto.recipientId().toString(),
                webSocketProperties.queueMessages(),
                eventDto
        );
    }

    private void handleTypingEvent(TypingEventDto typingDto) {
        String destination = String.format("/topic/chats/%s/typing", typingDto.chatId());

        log.debug("Processing typing event for chat={} from sender={}", typingDto.chatId(), typingDto.senderId());
        messagingTemplate.convertAndSend(destination, typingDto);
    }
}