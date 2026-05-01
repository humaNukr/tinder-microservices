package com.tinder.chat.chat.controller;

import com.tinder.chat.chat.dto.MediaInitRequest;
import com.tinder.chat.chat.dto.MediaInitResponse;
import com.tinder.chat.message.service.MessageFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatMediaController {

    private final MessageFacade messageFacade;

    @PostMapping("/{chatId}/media/init")
    @ResponseStatus(HttpStatus.OK)
    public MediaInitResponse initMediaUpload(
            @PathVariable UUID chatId,
            @RequestBody MediaInitRequest request,
            @RequestHeader("X-User-Id") UUID senderId
    ) {
        log.info("--- DEBUG STEP 1 [CONTROLLER] ---");
        log.info("Init media called for ChatId: {}", chatId);
        log.info("Header X-User-Id (Sender) received as: {}", senderId);
        return messageFacade.initMediaUpload(chatId, senderId, request);
    }
}