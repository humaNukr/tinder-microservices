package com.tinder.chat.application.port.out.message;

import com.tinder.chat.domain.model.Message;
import com.tinder.chat.shared.dto.common.CursorPage;

import java.util.UUID;

public interface MessagePersistencePort {
    Message save(Message message);

    Message getById(Long messageId);

    Message getByIdWithReactions(Long messageId);

    Message getPendingMessageByObjectKey(String objectKey);

    CursorPage<Message> getChatHistoryPage(UUID chatId, Long cursor, int limit);
}