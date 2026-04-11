package com.tinder.chat.message.controller;

import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.service.SendMessageOrchestrator;
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
    private final SendMessageOrchestrator orchestrator;

    @MessageMapping("/send")
    public void processIncomingMessage(@Payload @Valid ChatRequestDto requestDto, Principal principal) {
        log.info("Received a message from {} with chat id:{}", principal.getName(), requestDto.chatId());
        UUID senderId = UUID.fromString(principal.getName());
        orchestrator.execute(senderId, requestDto);
    }
}
