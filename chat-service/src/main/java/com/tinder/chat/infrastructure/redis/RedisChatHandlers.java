package com.tinder.chat.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.chat.dto.ReadReceiptEventDto;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.chat.port.ClientNotificationPort;
import com.tinder.chat.infrastructure.redis.contract.MessageDeletedEventDto;
import com.tinder.chat.message.dto.MessageEditedEventDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.dto.ReactionEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatHandlers {

    private final ObjectMapper objectMapper;
    private final ClientNotificationPort notificationPort;

    public void handleChatMessage(String messageBody) throws JsonProcessingException {
        MessageEventDto eventDto = objectMapper.readValue(messageBody, MessageEventDto.class);
        log.info("Processing chat message for recipient={}", eventDto.recipientId());
        notificationPort.sendNewMessage(eventDto);
    }

    public void handleTypingEvent(String messageBody) throws JsonProcessingException {
        TypingEventDto typingDto = objectMapper.readValue(messageBody, TypingEventDto.class);
        log.debug("Processing typing event for chat={}", typingDto.chatId());
        notificationPort.sendTypingEvent(typingDto);
    }

    public void handleReadReceipt(String messageBody) throws JsonProcessingException {
        ReadReceiptEventDto receiptDto = objectMapper.readValue(messageBody, ReadReceiptEventDto.class);
        notificationPort.sendReadReceipt(receiptDto);
    }

    public void handleReactionEvent(String messageBody) throws JsonProcessingException {
        ReactionEventDto eventDto = objectMapper.readValue(messageBody, ReactionEventDto.class);
        log.debug("Broadcasting reaction update: msgId={}, reaction={}", eventDto.messageId(), eventDto.reaction());
        notificationPort.sendReaction(eventDto);
    }

    public void handleMessageDeletedEvent(String messageBody) throws JsonProcessingException {
        MessageDeletedEventDto eventDto = objectMapper.readValue(messageBody, MessageDeletedEventDto.class);
        log.debug("Broadcasting message deletion to user {}: messageId {}", eventDto.recipientId(), eventDto.messageId());
        notificationPort.sendMessageDeleted(eventDto);
    }

    public void handleMessageEditedEvent(String messageBody) throws JsonProcessingException {
        MessageEditedEventDto eventDto = objectMapper.readValue(messageBody, MessageEditedEventDto.class);
        log.debug("Broadcasting message edit to user {}: messageId {}", eventDto.recipientId(), eventDto.messageId());
        notificationPort.sendMessageEdited(eventDto);
    }
}