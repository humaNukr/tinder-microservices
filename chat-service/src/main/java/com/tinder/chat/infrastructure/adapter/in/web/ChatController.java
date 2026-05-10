package com.tinder.chat.infrastructure.adapter.in.web;

import com.tinder.chat.application.port.in.room.GetChatHistoryQuery;
import com.tinder.chat.application.port.in.room.GetChatListQuery;
import com.tinder.chat.application.port.in.room.InitChatQuery;
import com.tinder.chat.shared.dto.room.ChatHistoryResponseDto;
import com.tinder.chat.shared.dto.room.ChatInitResponseDto;
import com.tinder.chat.shared.dto.room.ChatListItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final InitChatQuery initChatQuery;
    private final GetChatListQuery getChatListQuery;
    private final GetChatHistoryQuery getChatHistoryQuery;

    @GetMapping("/{chatId}/init")
    public ChatInitResponseDto initChat(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        int safeLimit = Math.min(limit, 50);
        return initChatQuery.initChat(chatId, userId, safeLimit);
    }

    @GetMapping("")
    public List<ChatListItemDto> getChatsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        int safeSize = Math.min(size, 50);
        int safePage = Math.max(page, 0);

        return getChatListQuery.getChatsList(userId, safePage, safeSize);
    }

    @GetMapping("/{chatId}/messages")
    public ChatHistoryResponseDto getChatHistory(
            @PathVariable UUID chatId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        int safeLimit = Math.min(limit, 50);
        return getChatHistoryQuery.getChatHistory(chatId, userId, cursor, safeLimit);
    }
}