package com.tinder.chat.chat.controller;

import com.tinder.chat.chat.dto.MediaInitRequest;
import com.tinder.chat.chat.dto.MediaInitResponse;
import com.tinder.chat.message.service.MessageFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
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
        return messageFacade.initMediaUpload(chatId, senderId, request);
    }

    @GetMapping("/{chatId}/media/{fileName}")
    public ResponseEntity<Void> getMedia(
            @PathVariable UUID chatId,
            @PathVariable String fileName,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.debug("Request to view media {} in chat {} by user {}", fileName, chatId, userId);

        String viewUrl = messageFacade.getMediaViewUrl(chatId, fileName, userId);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(viewUrl))
                .build();
    }
}