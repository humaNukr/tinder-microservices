package com.tinder.chat.chat.controller;

import com.tinder.chat.chat.dto.ChatHistoryResponseDto;
import com.tinder.chat.chat.dto.ChatInitResponseDto;
import com.tinder.chat.chat.dto.ChatListItemDto;
import com.tinder.chat.chat.service.ChatService;
import com.tinder.chat.message.service.MessageFacade;
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

    private final MessageFacade messageFacade;
    private final ChatService chatService;

    @GetMapping("/{chatId}/init")
    public ChatInitResponseDto initChat(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        int safeLimit = Math.min(limit, 50);
        return messageFacade.initChat(chatId, userId, safeLimit);
    }

    @GetMapping("")
    public List<ChatListItemDto> getChatsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        int safeSize = Math.min(size, 50);
        int safePage = Math.max(page, 0);

        return chatService.getChatsList(userId, safePage, safeSize);
    }

    @GetMapping("/{chatId}/messages")
    public ChatHistoryResponseDto getChatHistory(
            @PathVariable UUID chatId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        int safeLimit = Math.min(limit, 50);
        return messageFacade.getChatHistory(chatId, userId, cursor, safeLimit);
    }
}