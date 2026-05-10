package com.tinder.chat.application.service.message;

import com.tinder.chat.application.port.in.message.DeleteMessageUseCase;
import com.tinder.chat.application.port.in.message.EditMessageUseCase;
import com.tinder.chat.application.port.in.message.SendMessageUseCase;
import com.tinder.chat.application.port.out.common.IdempotencyPort;
import com.tinder.chat.application.port.out.message.MessageOutboxEventPort;
import com.tinder.chat.application.port.out.message.MessagePersistencePort;
import com.tinder.chat.application.port.out.notification.ChatEventPort;
import com.tinder.chat.application.port.out.notification.ClientNotificationPort;
import com.tinder.chat.application.port.out.room.ChatParticipantPersistencePort;
import com.tinder.chat.application.service.room.ChatRoomValidator;
import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.enums.MessageStatus;
import com.tinder.chat.domain.exception.AccessDeniedException;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.shared.dto.event.MessageAckDto;
import com.tinder.chat.shared.dto.message.ChatRequestDto;
import com.tinder.chat.shared.dto.message.EditMessageRequest;
import com.tinder.chat.shared.dto.message.MessageDeleteDto;
import com.tinder.chat.shared.mapper.MessageEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublishingService implements SendMessageUseCase, EditMessageUseCase, DeleteMessageUseCase {

    private final MessagePersistencePort persistencePort;
    private final MessageOutboxEventPort outboxEventPort;
    private final ChatRoomValidator chatRoomValidator;
    private final ChatParticipantPersistencePort participantPersistencePort;
    private final ChatEventPort eventPort;
    private final ClientNotificationPort notificationPort;
    private final IdempotencyPort idempotencyPort;
    private final MessageEventMapper messageEventMapper;

    @Override
    @Transactional
    public void saveMessage(UUID senderId, ChatRequestDto requestDto) {
        String idempotencyKey = "chat:idempotency:" + senderId + ":" + requestDto.localId();
        Duration ttl = Duration.ofHours(24);

        if (!idempotencyPort.tryAcquire(idempotencyKey, ttl)) {
            handleIdempotencyCollision(senderId, requestDto.localId(), idempotencyKey);
            return;
        }

        Set<UUID> participants = chatRoomValidator.validateAndGetParticipants(requestDto.chatId(), senderId);
        UUID partnerId = chatRoomValidator.getPartnerId(participants, senderId);

        Message parentMessage = null;
        if (requestDto.replyToMessageId() != null) {
            parentMessage = persistencePort.getById(requestDto.replyToMessageId());
            if (!parentMessage.getChatId().equals(requestDto.chatId())) {
                throw new IllegalArgumentException("Parent message belongs to another chat");
            }
        }

        MessageContentType contentType = MessageContentType.valueOf(requestDto.type().toUpperCase());
        Message message = Message.builder()
                .chatId(requestDto.chatId())
                .senderId(senderId)
                .contentType(contentType)
                .content(requestDto.payload())
                .status(MessageStatus.SENT)
                .parentMessage(parentMessage)
                .build();

        Message savedMessage = persistencePort.save(message);
        outboxEventPort.publishMessageSavedEvent(savedMessage, partnerId);

        participantPersistencePort.updateLastReadMessageIdIfGreater(requestDto.chatId(), senderId, savedMessage.getId());

        eventPort.publishNewMessage(messageEventMapper.toEventDto(savedMessage, partnerId));
        idempotencyPort.complete(idempotencyKey, savedMessage.getId().toString(), ttl);
        notificationPort.sendAck(senderId, messageEventMapper.toAckDto(savedMessage, requestDto.localId()));
    }

    @Override
    @Transactional
    public void editMessage(UUID senderId, EditMessageRequest requestDto) {
        Message message = persistencePort.getById(requestDto.messageId());
        Set<UUID> participants = chatRoomValidator.validateAndGetParticipants(requestDto.chatId(), senderId);
        UUID partnerId = chatRoomValidator.getPartnerId(participants, senderId);

        if (!message.getSenderId().equals(senderId)) {
            throw new AccessDeniedException("You can only edit your own messages");
        }
        if (message.getStatus() == MessageStatus.DELETED || message.getContentType() != MessageContentType.TEXT) {
            throw new IllegalStateException("Cannot edit this type of message");
        }

        message.edit(requestDto.newContent());
        Message savedMessage = persistencePort.save(message);

        eventPort.publishMessageEdited(messageEventMapper.toEditedEventDto(savedMessage, partnerId));
    }

    @Override
    @Transactional
    public void deleteMessage(UUID senderId, MessageDeleteDto requestDto) {
        Set<UUID> participants = chatRoomValidator.validateAndGetParticipants(requestDto.chatId(), senderId);
        UUID partnerId = chatRoomValidator.getPartnerId(participants, senderId);

        Message message = persistencePort.getById(requestDto.messageId());

        if (!message.getSenderId().equals(senderId)) {
            throw new AccessDeniedException("You can only delete your own messages");
        }
        if (!message.getChatId().equals(requestDto.chatId())) {
            throw new IllegalArgumentException("Message does not belong to this chat");
        }

        if (message.isDeleted()) {
            return;
        }

        message.markAsDeleted();
        persistencePort.save(message);

        eventPort.publishMessageDeleted(messageEventMapper.toDeletedEventDto(message, partnerId));
    }

    private void handleIdempotencyCollision(UUID senderId, UUID localId, String idempotencyKey) {
        log.warn("Idempotent request detected for localId: {}", localId);
        String existingResult = idempotencyPort.getResult(idempotencyKey);

        Long dbIdToReturn = ("PENDING".equals(existingResult) || existingResult == null)
                ? null
                : Long.valueOf(existingResult);

        Instant createdAtToReturn = Instant.now();

        if (dbIdToReturn != null) {
            Message existingMessage = persistencePort.getById(dbIdToReturn);
            createdAtToReturn = existingMessage.getCreatedAt();
        }

        notificationPort.sendAck(senderId, new MessageAckDto(
                localId,
                dbIdToReturn,
                createdAtToReturn
        ));
    }
}