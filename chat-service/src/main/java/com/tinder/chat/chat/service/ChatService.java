package com.tinder.chat.chat.service;

import java.util.UUID;

public interface ChatService {
    void createChat(UUID user1Id, UUID user2Id);
    UUID validateAndGetRecipientId(UUID chatId, UUID senderId);
}
