package com.tinder.chat.application.port.in.room;

import com.tinder.chat.shared.dto.room.ChatInitResponseDto;

import java.util.UUID;

public interface InitChatQuery {
    ChatInitResponseDto initChat(UUID chatId, UUID userId, int limit);
}