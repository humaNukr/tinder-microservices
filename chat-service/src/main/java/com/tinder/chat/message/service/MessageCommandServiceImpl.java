package com.tinder.chat.message.service;

import com.tinder.chat.chat.dto.MediaInitRequest;
import com.tinder.chat.chat.dto.MediaInitResponse;
import com.tinder.chat.chat.port.ChatEventPublisher;
import com.tinder.chat.chat.port.ChatParticipantProvider;
import com.tinder.chat.chat.port.ClientNotificationPort;
import com.tinder.chat.chat.port.IdempotencyPort;
import com.tinder.chat.chat.service.ChatParticipantService;
import com.tinder.chat.exception.AccessDeniedException;
import com.tinder.chat.infrastructure.storage.StorageService;
import com.tinder.chat.message.dto.ChatRequestDto;
import com.tinder.chat.message.dto.MessageAckDto;
import com.tinder.chat.message.dto.MessageDeleteDto;
import com.tinder.chat.message.dto.ReactionEventDto;
import com.tinder.chat.message.dto.ReactionRequestDto;
import com.tinder.chat.message.enums.MessageContentType;
import com.tinder.chat.message.enums.MessageStatus;
import com.tinder.chat.message.mapper.MessageMapper;
import com.tinder.chat.message.model.Message;
import com.tinder.chat.message.model.MessageReaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageCommandServiceImpl implements MessageCommandService {

    private final MessageService messageService;
    private final ChatParticipantProvider participantProvider;
    private final ChatParticipantService participantService;
    private final StorageService storageService;
    private final ChatEventPublisher eventPublisher;
    private final ClientNotificationPort clientNotificationPort;
    private final IdempotencyPort idempotencyPort;
    private final MessageMapper messageMapper;

    @Transactional
    public void saveMessage(UUID senderId, ChatRequestDto requestDto) {
        String idempotencyKey = "chat:idempotency:" + senderId + ":" + requestDto.localId();
        Duration ttl = Duration.ofHours(24);

        if (!idempotencyPort.tryAcquire(idempotencyKey, ttl)) {
            log.warn("Idempotent request detected for localId: {}", requestDto.localId());
            String existingResult = idempotencyPort.getResult(idempotencyKey);

            Long dbIdToReturn = ("PENDING".equals(existingResult) || existingResult == null)
                    ? null
                    : Long.valueOf(existingResult);

            Instant createdAtToReturn = Instant.now();
            if (dbIdToReturn != null) {
                Message existingMessage = messageService.getMessageById(dbIdToReturn);
                createdAtToReturn = existingMessage.getCreatedAt();
            }

            clientNotificationPort.sendAck(senderId, new MessageAckDto(
                    requestDto.localId(),
                    dbIdToReturn,
                    createdAtToReturn
            ));
            return;
        }

        Set<UUID> participants = getParticipantsAndValidate(requestDto.chatId(), senderId);
        UUID recipientId = getPartnerId(participants, senderId);

        Message parentMessage = null;
        if (requestDto.replyToMessageId() != null) {
            parentMessage = messageService.getMessageById(requestDto.replyToMessageId());

            if (!parentMessage.getChatId().equals(requestDto.chatId())) {
                throw new IllegalArgumentException("Parent message belongs to another chat");
            }
        }

        Message savedMessage = messageService.saveReadyMessage(senderId, recipientId, requestDto, parentMessage);
        participantService.updateWatermark(requestDto.chatId(), senderId, savedMessage.getId());

        eventPublisher.publishNewMessage(messageMapper.toEventDto(savedMessage, recipientId));

        idempotencyPort.complete(idempotencyKey, savedMessage.getId().toString(), ttl);

        clientNotificationPort.sendAck(senderId, messageMapper.toAckDto(savedMessage, requestDto.localId()));
    }

    @Transactional
    public void deleteMessage(UUID senderId, MessageDeleteDto request) {
        Set<UUID> participants = getParticipantsAndValidate(request.chatId(), senderId);
        UUID partnerId = getPartnerId(participants, senderId);

        Message message = messageService.getMessageById(request.messageId());

        if (!message.getSenderId().equals(senderId)) {
            throw new AccessDeniedException("You can only delete your own messages");
        }
        if (!message.getChatId().equals(request.chatId())) {
            throw new IllegalArgumentException("Message does not belong to this chat");
        }

        if (message.isDeleted()) {
            return;
        }

        message.markAsDeleted();

        eventPublisher.publishMessageDeleted(messageMapper.toDeletedEventDto(message, partnerId));
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

    @Transactional
    public void confirmMediaUpload(String objectKey) {
        Message message = messageService.getPendingMessageByObjectKey(objectKey);

        if (message.getStatus() == MessageStatus.SENT) {
            return;
        }

        Set<UUID> participants = getParticipantsAndValidate(message.getChatId(), message.getSenderId());
        UUID recipientId = getPartnerId(participants, message.getSenderId());

        Message confirmedMessage = messageService.markMessageAsSentAndPublishOutbox(message, recipientId);

        eventPublisher.publishNewMessage(messageMapper.toEventDto(confirmedMessage, recipientId));
        clientNotificationPort.sendAck(message.getSenderId(), messageMapper.toAckDto(confirmedMessage, null));
    }

    @Transactional
    public void toggleReaction(UUID senderId, ReactionRequestDto request) {
        Set<UUID> participants = getParticipantsAndValidate(request.chatId(), senderId);
        UUID partnerId = getPartnerId(participants, senderId);

        Message message = messageService.getMessageById(request.messageId());

        if (!message.getChatId().equals(request.chatId())) {
            throw new IllegalArgumentException("Message does not belong to this chat");
        }
        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot react to a deleted message");
        }

        String finalReaction = null;

        Optional<MessageReaction> existingReactionOpt = message.getReactions().stream()
                .filter(r -> r.getUserId().equals(senderId))
                .findFirst();

        if (existingReactionOpt.isPresent()) {
            MessageReaction existingReaction = existingReactionOpt.get();
            if (existingReaction.getReaction().equals(request.reaction())) {
                message.removeReaction(existingReaction);
            } else {
                existingReaction.setReaction(request.reaction());
                finalReaction = request.reaction();
            }
        } else {
            MessageReaction newReaction = MessageReaction.builder()
                    .userId(senderId)
                    .reaction(request.reaction())
                    .build();
            message.addReaction(newReaction);
            finalReaction = request.reaction();
        }

        ReactionEventDto eventDto = new ReactionEventDto(
                request.chatId(),
                request.messageId(),
                senderId,
                partnerId,
                finalReaction
        );
        eventPublisher.publishReaction(eventDto);
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

    private String buildObjectKey(UUID chatId, UUID fileId, String extension) {
        return String.format("chats/%s/%s%s", chatId, fileId, extension);
    }
}