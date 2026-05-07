package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.CursorPage;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.enums.MessageContentType;
import com.tinder.chat.message.model.Message;

import java.util.UUID;

public interface MessageService {
    Message saveReadyMessage(UUID senderId, UUID recipientId, ChatRequestDto requestDto, Message parentMessage);

    Message savePendingMessage(UUID chatId, UUID senderId, MessageContentType type, String objectKey);

    Message markMessageAsSentAndPublishOutbox(Message message, UUID recipientId);

    Message getMessageById(Long messageId);

    Message getMessageByIdWithReactions(Long messageId);

    CursorPage<Message> getChatHistoryPage(UUID chatId, Long cursor, int limit);

    Message getPendingMessageByObjectKey(String objectKey);
}
