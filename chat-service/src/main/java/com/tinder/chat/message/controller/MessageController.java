package com.tinder.chat.message.controller;

import com.tinder.chat.chat.dto.ReadReceiptRequest;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.dto.MessageDeleteDto;
import com.tinder.chat.message.service.MessageFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@MessageMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    private final MessageFacade facade;

    @MessageMapping("/send")
    public void processIncomingMessage(@Payload @Valid ChatRequestDto requestDto, Principal principal) {
        log.info("Received a message from {} with chat id:{}", principal.getName(), requestDto.chatId());
        UUID senderId = UUID.fromString(principal.getName());
        facade.saveMessage(senderId, requestDto);
    }

    @MessageMapping("/typing")
    public void processTypingEvent(@Payload TypingEventDto requestDto, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        facade.processTypingEvent(requestDto, senderId);
    }

    @MessageMapping("/read")
    public void processReadReceipt(@Payload ReadReceiptRequest request, Principal principal) {
        UUID readerId = UUID.fromString(principal.getName());
        facade.processReadReceipt(readerId, request);
    }

    @MessageMapping("/delete")
    public void processDeleteMessage(@Payload @Valid MessageDeleteDto request, Principal principal) {
        log.info("User {} deleting message {} in chat {}", principal.getName(), request.messageId(), request.chatId());
        UUID senderId = UUID.fromString(principal.getName());
        facade.deleteMessage(senderId, request);
    }
}
