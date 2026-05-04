package com.tinder.chat.chat.controller;

import com.tinder.chat.chat.dto.ChatHistoryResponseDto;
import com.tinder.chat.chat.dto.ChatInitResponseDto;
import com.tinder.chat.message.service.MessageFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final MessageFacade messageFacade;

    @GetMapping("/{chatId}/init")
    public ChatInitResponseDto initChat(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        int safeLimit = Math.min(limit, 50);
        return messageFacade.initChat(chatId, userId, safeLimit);
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