package com.tinder.chat.infrastructure.websocket;

import com.tinder.chat.chat.dto.ReadReceiptEventDto;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.chat.port.ClientNotificationPort;
import com.tinder.chat.config.WebSocketProperties;
import com.tinder.chat.infrastructure.redis.contract.MessageDeletedEventDto;
import com.tinder.chat.message.dto.MessageAckDto;
import com.tinder.chat.message.dto.MessageEditedEventDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.dto.ReactionEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompClientNotificationAdapter implements ClientNotificationPort {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties webSocketProperties;

    @Override
    public void sendAck(UUID userId, MessageAckDto ackDto) {
        log.debug("Sending message ACK to user {}: localId={}", userId, ackDto.localId());
        messagingTemplate.convertAndSendToUser(userId.toString(), webSocketProperties.queueAcks(), ackDto);
    }

    @Override
    public void sendNewMessage(MessageEventDto eventDto) {
        messagingTemplate.convertAndSendToUser(eventDto.recipientId().toString(), webSocketProperties.queueMessages(), eventDto);
    }

    @Override
    public void sendTypingEvent(TypingEventDto eventDto) {
        String destination = String.format("/topic/chats/%s/typing", eventDto.chatId());
        messagingTemplate.convertAndSend(destination, eventDto);
    }

    @Override
    public void sendReadReceipt(ReadReceiptEventDto eventDto) {
        messagingTemplate.convertAndSendToUser(eventDto.recipientId().toString(), webSocketProperties.queueReadReceipts(), eventDto);
    }

    @Override
    public void sendReaction(ReactionEventDto eventDto) {
        messagingTemplate.convertAndSendToUser(eventDto.recipientId().toString(), webSocketProperties.queueMessageReactions(), eventDto);
        messagingTemplate.convertAndSendToUser(eventDto.senderId().toString(), webSocketProperties.queueMessageReactions(), eventDto);
    }

    @Override
    public void sendMessageDeleted(MessageDeletedEventDto eventDto) {
        messagingTemplate.convertAndSendToUser(eventDto.recipientId().toString(), webSocketProperties.queueMessageDeletions(), eventDto);
        messagingTemplate.convertAndSendToUser(eventDto.senderId().toString(), webSocketProperties.queueMessageDeletions(), eventDto);
    }

    @Override
    public void sendMessageEdited(MessageEditedEventDto eventDto) {
        messagingTemplate.convertAndSendToUser(eventDto.recipientId().toString(), webSocketProperties.queueMessageEdits(), eventDto);
        messagingTemplate.convertAndSendToUser(eventDto.senderId().toString(), webSocketProperties.queueMessageEdits(), eventDto);
    }
}