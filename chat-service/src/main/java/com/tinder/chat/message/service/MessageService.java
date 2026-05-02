package com.tinder.chat.message.service;

import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.enums.MessageContentType;
import com.tinder.chat.message.model.Message;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    Message saveReadyMessage(UUID senderId, UUID recipientId, ChatRequestDto requestDto);

    Message savePendingMessage(UUID chatId, UUID senderId, MessageContentType type, String objectKey);

    Message markMessageAsSentAndPublishOutbox(Message message, UUID recipientId);

    Message getMessageById(Long messageId);

    List<Message> getChatHistory(UUID chatId, Long cursorId, int limit);

    Message getPendingMessageByObjectKey(String objectKey);
}
