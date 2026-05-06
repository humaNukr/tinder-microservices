package com.tinder.chat.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.chat.dto.ReadReceiptEventDto;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.config.WebSocketProperties;
import com.tinder.chat.infrastructure.redis.contract.MessageDeletedEventDto;
import com.tinder.chat.message.dto.MessageEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatHandlers {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    public void handleChatMessage(String messageBody) throws JsonProcessingException {
        MessageEventDto eventDto = objectMapper.readValue(messageBody, MessageEventDto.class);
        log.info("Processing chat message for recipient={}", eventDto.recipientId());

        messagingTemplate.convertAndSendToUser(
                eventDto.recipientId().toString(),
                webSocketProperties.queueMessages(),
                eventDto
        );
    }

    public void handleTypingEvent(String messageBody) throws JsonProcessingException {
        TypingEventDto typingDto = objectMapper.readValue(messageBody, TypingEventDto.class);
        String destination = String.format("/topic/chats/%s/typing", typingDto.chatId());

        log.debug("Processing typing event for chat={}", typingDto.chatId());
        messagingTemplate.convertAndSend(destination, typingDto);
    }

    public void handleReadReceipt(String messageBody) throws JsonProcessingException {
        ReadReceiptEventDto receiptDto = objectMapper.readValue(messageBody, ReadReceiptEventDto.class);
        messagingTemplate.convertAndSendToUser(
                receiptDto.recipientId().toString(),
                webSocketProperties.queueReadReceipts(),
                receiptDto
        );
    }

    public void handleMessageDeletedEvent(String messageBody) throws JsonProcessingException {
        MessageDeletedEventDto eventDto = objectMapper.readValue(messageBody, MessageDeletedEventDto.class);
        log.debug("Broadcasting message deletion to user {}: messageId {}", eventDto.recipientId(), eventDto.messageId());

        messagingTemplate.convertAndSendToUser(
                eventDto.recipientId().toString(),
                "/queue/message-deletions",
                eventDto
        );
    }
}