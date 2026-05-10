package com.tinder.chat.infrastructure.adapter.in.websocket;

import com.tinder.chat.application.port.in.activity.ProcessReadReceiptUseCase;
import com.tinder.chat.application.port.in.activity.SendTypingEventUseCase;
import com.tinder.chat.application.port.in.message.DeleteMessageUseCase;
import com.tinder.chat.application.port.in.message.EditMessageUseCase;
import com.tinder.chat.application.port.in.message.SendMessageUseCase;
import com.tinder.chat.application.port.in.message.ToggleReactionUseCase;
import com.tinder.chat.shared.dto.activity.ReadReceiptRequest;
import com.tinder.chat.shared.dto.event.TypingEventDto;
import com.tinder.chat.shared.dto.message.ChatRequestDto;
import com.tinder.chat.shared.dto.message.EditMessageRequest;
import com.tinder.chat.shared.dto.message.MessageDeleteDto;
import com.tinder.chat.shared.dto.message.ReactionRequestDto;
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
public class MessageWebSocketController {

    private final SendMessageUseCase sendMessageUseCase;
    private final EditMessageUseCase editMessageUseCase;
    private final DeleteMessageUseCase deleteMessageUseCase;
    private final ToggleReactionUseCase toggleReactionUseCase;
    private final ProcessReadReceiptUseCase processReadReceiptUseCase;
    private final SendTypingEventUseCase sendTypingEventUseCase;

    @MessageMapping("/send")
    public void processIncomingMessage(@Payload @Valid ChatRequestDto requestDto, Principal principal) {
        log.info("Received a message from {} with chat id:{}", principal.getName(), requestDto.chatId());
        UUID senderId = UUID.fromString(principal.getName());
        sendMessageUseCase.saveMessage(senderId, requestDto);
    }

    @MessageMapping("/edit")
    public void editMessage(@Payload @Valid EditMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        editMessageUseCase.editMessage(senderId, request);
    }

    @MessageMapping("/typing")
    public void processTypingEvent(@Payload TypingEventDto requestDto, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        sendTypingEventUseCase.processTypingEvent(requestDto, senderId);
    }

    @MessageMapping("/read")
    public void processReadReceipt(@Payload ReadReceiptRequest request, Principal principal) {
        UUID readerId = UUID.fromString(principal.getName());
        processReadReceiptUseCase.processReadReceipt(readerId, request);
    }

    @MessageMapping("/react")
    public void reactToMessage(Principal principal, @Payload @Valid ReactionRequestDto request) {
        UUID senderId = UUID.fromString(principal.getName());
        toggleReactionUseCase.toggleReaction(senderId, request);
    }

    @MessageMapping("/delete")
    public void processDeleteMessage(@Payload @Valid MessageDeleteDto request, Principal principal) {
        log.info("User {} deleting message {} in chat {}", principal.getName(), request.messageId(), request.chatId());
        UUID senderId = UUID.fromString(principal.getName());
        deleteMessageUseCase.deleteMessage(senderId, request);
    }
}