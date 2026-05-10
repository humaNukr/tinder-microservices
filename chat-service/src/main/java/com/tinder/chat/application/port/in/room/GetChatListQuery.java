package com.tinder.chat.application.port.in.room;

import com.tinder.chat.shared.dto.room.ChatListItemDto;

import java.util.List;
import java.util.UUID;

public interface GetChatListQuery {
    List<ChatListItemDto> getChatsList(UUID myUserId, int page, int size);
}