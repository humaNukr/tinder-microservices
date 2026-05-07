package com.tinder.chat.chat.service;

import com.tinder.chat.chat.dto.ChatListItemDto;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    void createChat(UUID user1Id, UUID user2Id);

    List<ChatListItemDto> getChatsList(UUID myUserId, int page, int size);
}
