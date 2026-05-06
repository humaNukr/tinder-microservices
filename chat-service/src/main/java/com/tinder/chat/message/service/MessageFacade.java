package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.ChatHistoryResponseDto;
import com.tinder.chat.chat.dto.ChatInitResponseDto;
import com.tinder.chat.chat.dto.MediaInitRequest;
import com.tinder.chat.chat.dto.MediaInitResponse;
import com.tinder.chat.chat.dto.ReadReceiptEventDto;
import com.tinder.chat.chat.dto.ReadReceiptRequest;
import com.tinder.chat.chat.dto.TypingEventDto;
import com.tinder.chat.chat.port.ChatEventPublisher;
import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.chat.port.ClientNotificationPort;
import com.tinder.chat.chat.service.ChatParticipantService;
import com.tinder.chat.exception.AccessDeniedException;
import com.tinder.chat.infrastructure.storage.StorageService;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.dto.MessageAckDto;
import com.tinder.chat.message.dto.MessageEventDto;
import com.tinder.chat.message.dto.MessageResponseDto;
import com.tinder.chat.message.enums.MessageContentType;
import com.tinder.chat.message.enums.MessageStatus;
import com.tinder.chat.message.model.Message;
import com.tinder.chat.user.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageFacade {

    private final ChatParticipantProvider participantProvider;
    private final MessageService messageService;
    private final ChatEventPublisher eventPublisher;
    private final StorageService storageService;
    private final ClientNotificationPort clientNotificationPort;
    private final ChatParticipantService participantService;
    private final UserPresenceService userPresenceService;

    @Transactional
    public void saveMessage(UUID senderId, ChatRequestDto requestDto) {
        Set<UUID> participants = getParticipantsAndValidate(requestDto.chatId(), senderId);
        UUID recipientId = getPartnerId(participants, senderId);

        Message savedMessage = messageService.saveReadyMessage(senderId, recipientId, requestDto);

        participantService.updateWatermark(requestDto.chatId(), senderId, savedMessage.getId());

        publishMessageEvent(savedMessage, recipientId);

        MessageAckDto ack = new MessageAckDto(
                requestDto.localId(),
                savedMessage.getId(),
                savedMessage.getCreatedAt()
        );
        clientNotificationPort.sendAck(senderId, ack);
    }

    @Transactional
    public MediaInitResponse initMediaUpload(UUID chatId, UUID senderId, MediaInitRequest request) {
        getParticipantsAndValidate(chatId, senderId);

        UUID fileId = UUID.randomUUID();
        String objectKey = buildObjectKey(chatId, fileId, request.fileExtension());
        MessageContentType contentType = MessageContentType.valueOf(request.type().toUpperCase());

        Message pendingMessage = messageService.savePendingMessage(chatId, senderId, contentType, objectKey);
        String uploadUrl = storageService.generateTempLinkForUploading(objectKey);

        participantService.updateWatermark(chatId, senderId, pendingMessage.getId());

        return new MediaInitResponse(
                request.localId(),
                pendingMessage.getId(),
                uploadUrl
        );
    }

    @Transactional(readOnly = true)
    public ChatInitResponseDto initChat(UUID chatId, UUID userId, int limit) {
        Set<UUID> participants = getParticipantsAndValidate(chatId, userId);
        UUID partnerId = getPartnerId(participants, userId);

        ChatHistoryResponseDto historyPage = getChatHistoryInternal(chatId, null, limit);

        boolean isPartnerOnline = userPresenceService.isUserOnline(partnerId);
        Long partnerWatermark = participantService.getParticipantWatermark(chatId, partnerId);
        Long myWatermark = participantService.getParticipantWatermark(chatId, userId);

        return new ChatInitResponseDto(
                historyPage.messages(),
                isPartnerOnline,
                partnerWatermark,
                myWatermark,
                historyPage.nextCursor(),
                historyPage.hasNext()
        );
    }

    @Transactional
    public void confirmMediaUpload(String objectKey) {
        Message message = messageService.getPendingMessageByObjectKey(objectKey);

        if (message.getStatus() == MessageStatus.SENT) {
            return;
        }

        Set<UUID> participants = getParticipantsAndValidate(message.getChatId(), message.getSenderId());
        UUID recipientId = getPartnerId(participants, message.getSenderId());

        Message confirmedMessage = messageService.markMessageAsSentAndPublishOutbox(message, recipientId);

        publishMessageEvent(confirmedMessage, recipientId);

        MessageAckDto ack = new MessageAckDto(
                null,
                confirmedMessage.getId(),
                confirmedMessage.getCreatedAt()
        );
        clientNotificationPort.sendAck(message.getSenderId(), ack);
    }

    @Transactional(readOnly = true)
    public String getMediaViewUrl(UUID chatId, String fileName, UUID userId) {
        getParticipantsAndValidate(chatId, userId);

        String objectKey = String.format("chats/%s/%s", chatId, fileName);
        return storageService.generateTempLinkForViewing(objectKey);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponseDto getChatHistory(UUID chatId, UUID userId, Long cursor, int limit) {
        getParticipantsAndValidate(chatId, userId);
        return getChatHistoryInternal(chatId, cursor, limit);
    }

    @Transactional
    public void processReadReceipt(UUID readerId, ReadReceiptRequest request) {
        int updatedRows = participantService.updateWatermark(request.chatId(), readerId, request.messageId());

        if (updatedRows == 0) {
            return;
        }

        Set<UUID> participants = getParticipantsAndValidate(request.chatId(), readerId);
        UUID recipientId = getPartnerId(participants, readerId);

        ReadReceiptEventDto eventDto = new ReadReceiptEventDto(
                request.chatId(),
                readerId,
                recipientId,
                request.messageId()
        );
        eventPublisher.publishReadReceipt(eventDto);
    }

    public void processTypingEvent(TypingEventDto requestDto, UUID senderId) {
        eventPublisher.publishTypingEvent(new TypingEventDto(requestDto.chatId(), senderId));
    }

    private ChatHistoryResponseDto getChatHistoryInternal(UUID chatId, Long cursor, int limit) {
        int limitPlusOne = limit + 1;
        List<Message> messages = new ArrayList<>(messageService.getChatHistory(chatId, cursor, limitPlusOne));

        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages.remove(limit);
        }

        List<MessageResponseDto> messageDtos = messages.stream()
                .map(this::toMessageResponseDto)
                .toList();

        Long nextCursor = messageDtos.isEmpty() ? null : messageDtos.getLast().id();

        return new ChatHistoryResponseDto(messageDtos, nextCursor, hasNext);
    }

    private Set<UUID> getParticipantsAndValidate(UUID chatId, UUID userId) {
        Set<UUID> participants = participantProvider.getParticipants(chatId);
        if (!participants.contains(userId)) {
            throw new AccessDeniedException("User is not a participant of this chat");
        }
        return participants;
    }

    private UUID getPartnerId(Set<UUID> participants, UUID userId) {
        return participants.stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Recipient not found"));
    }

    private void publishMessageEvent(Message message, UUID recipientId) {
        MessageEventDto eventDto = new MessageEventDto(
                message.getId(),
                message.getChatId(),
                message.getSenderId(),
                recipientId,
                message.getContentType().name(),
                resolveContentUrl(message),
                message.getCreatedAt()
        );
        eventPublisher.publishNewMessage(eventDto);
    }

    private MessageResponseDto toMessageResponseDto(Message message) {
        return new MessageResponseDto(
                message.getId(),
                message.getSenderId(),
                message.getContentType(),
                resolveContentUrl(message),
                message.getCreatedAt()
        );
    }

    private String resolveContentUrl(Message message) {
        String content = message.getContent();
        if (message.getContentType() != MessageContentType.TEXT) {
            String[] parts = content.split("/");
            String fileNameWithExt = parts[parts.length - 1];
            return String.format("/api/v1/chats/%s/media/%s", message.getChatId(), fileNameWithExt);
        }
        return content;
    }

    private String buildObjectKey(UUID chatId, UUID fileId, String extension) {
        return String.format("chats/%s/%s%s", chatId, fileId, extension);
    }
}