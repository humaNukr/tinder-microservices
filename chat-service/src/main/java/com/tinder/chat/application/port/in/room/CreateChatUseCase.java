package com.tinder.chat.application.port.in.room;

import java.util.UUID;

public interface CreateChatUseCase {
    void createChat(UUID user1Id, UUID user2Id);
}