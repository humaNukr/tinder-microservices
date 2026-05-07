package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.ChatHistoryResponseDto;
import com.tinder.chat.chat.dto.ChatInitResponseDto;

import java.util.UUID;

public interface MessageQueryService {

    ChatInitResponseDto initChat(UUID chatId, UUID userId, int limit);

    ChatHistoryResponseDto getChatHistory(UUID chatId, UUID userId, Long cursor, int limit);

    String getMediaViewUrl(UUID chatId, String fileName, UUID userId);
}
