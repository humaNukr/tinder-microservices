package com.tinder.chat.application.port.out.room;

import com.tinder.chat.domain.model.Chat;
import com.tinder.chat.domain.model.ChatPreview;

import java.util.List;
import java.util.UUID;

public interface ChatPersistencePort {
    Chat save(Chat chat);

    List<ChatPreview> findChatPreviewsByUserId(UUID userId, int limit, int offset);
}