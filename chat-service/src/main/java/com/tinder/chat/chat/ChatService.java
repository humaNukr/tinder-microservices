package com.tinder.chat.chat;

import java.util.UUID;

public interface ChatService {
    void createChat(UUID user1Id, UUID user2Id);
}
