package com.tinder.chat.message.service;

import com.tinder.chat.chat.port.ChatEventPublisher;
import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.exception.AccessDeniedException;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SendMessageOrchestrator {

    private final ChatParticipantProvider participantProvider;
    private final MessageService messageService;
    private final ChatEventPublisher eventPublisher;

    @Transactional
    public void execute(UUID senderId, ChatRequestDto requestDto) {

        UUID chatId = requestDto.chatId();

        Set<UUID> participants = participantProvider.getParticipants(chatId);

        if (!participants.contains(senderId)) {
            throw new AccessDeniedException("User is not a participant of this chat");
        }

        UUID recipientId = participants.stream()
                .filter(id -> !id.equals(senderId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Recipient not found"));

        Message savedMessage = messageService.saveMessage(senderId, recipientId, requestDto);

        MessageEventDto eventDto = new MessageEventDto(
                savedMessage.getId(),
                savedMessage.getChatId(),
                senderId,
                recipientId,
                savedMessage.getContentType().name(),
                savedMessage.getContent(),
                savedMessage.getCreatedAt()
        );

        eventPublisher.publishNewMessage(eventDto);
    }
}