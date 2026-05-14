package com.tinder.chat.application.port.in.room;

import com.tinder.chat.shared.dto.room.ChatHistoryResponseDto;

import java.util.UUID;

public interface GetChatHistoryQuery {
    ChatHistoryResponseDto getChatHistory(UUID chatId, UUID userId, Long cursor, int limit);
}