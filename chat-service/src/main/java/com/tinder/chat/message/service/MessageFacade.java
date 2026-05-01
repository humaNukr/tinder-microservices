package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.MediaInitRequest;
import com.tinder.chat.chat.dto.MediaInitResponse;
import com.tinder.chat.chat.port.ChatEventPublisher;
import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.exception.AccessDeniedException;
import com.tinder.chat.infrastructure.storage.StorageService;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.enums.MessageStatus;
import com.tinder.chat.message.mapper.MessageMapper;
import com.tinder.chat.message.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageFacade {

    private final ChatParticipantProvider participantProvider;
    private final MessageService messageService;
    private final ChatEventPublisher eventPublisher;
    private final StorageService storageService;
    private final MessageMapper messageMapper;

    @Transactional
    public void saveMessage(UUID senderId, ChatRequestDto requestDto) {
        UUID recipientId = resolveRecipientId(requestDto.chatId(), senderId);
        Message savedMessage = messageService.saveReadyMessage(senderId, recipientId, requestDto);
        publishMessageEvent(savedMessage, recipientId);
    }

    @Transactional
    public MediaInitResponse initMediaUpload(UUID chatId, UUID senderId, MediaInitRequest request) {
        validateParticipant(chatId, senderId);

        UUID fileId = UUID.randomUUID();
        String objectKey = buildObjectKey(chatId, fileId, request.fileExtension());

        Message pendingMessage = messageService.savePendingMessage(chatId, senderId, request.type(), objectKey);
        String uploadUrl = storageService.generateTempLinkForUploading(objectKey);

        return new MediaInitResponse(pendingMessage.getId(), uploadUrl);
    }

    @Transactional
    public void confirmMediaUpload(String objectKey) {
        Message message = messageService.getPendingMessageByObjectKey(objectKey);

        if (message.getStatus() == MessageStatus.SENT) {
            return;
        }

        UUID recipientId = resolveRecipientId(message.getChatId(), message.getSenderId());
        Message confirmedMessage = messageService.markMessageAsSentAndPublishOutbox(message, recipientId);

        publishMessageEvent(confirmedMessage, recipientId);
    }


    private UUID resolveRecipientId(UUID chatId, UUID senderId) {
        Set<UUID> participants = participantProvider.getParticipants(chatId);

        if (!participants.contains(senderId)) {
            throw new AccessDeniedException("User is not a participant of this chat");
        }

        return participants.stream()
                .filter(id -> !id.equals(senderId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Recipient not found"));
    }

    private void publishMessageEvent(Message message, UUID recipientId) {
        MessageEventDto eventDto = messageMapper.toEventDto(message, recipientId);
        eventPublisher.publishNewMessage(eventDto);
    }

    private String buildObjectKey(UUID chatId, UUID fileId, String extension) {
        return String.format("chats/%s/%s%s", chatId, fileId, extension);
    }

    private void validateParticipant(UUID chatId, UUID senderId) {
        if (!participantProvider.getParticipants(chatId).contains(senderId)) {
            throw new AccessDeniedException("User is not a participant of this chat");
        }
    }


}