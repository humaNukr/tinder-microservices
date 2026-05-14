package com.tinder.chat.application.service.media;

import com.tinder.chat.application.port.in.media.ConfirmMediaUploadUseCase;
import com.tinder.chat.application.port.in.media.InitMediaUploadUseCase;
import com.tinder.chat.application.port.out.media.MediaStoragePort;
import com.tinder.chat.application.port.out.message.MessageOutboxEventPort;
import com.tinder.chat.application.port.out.message.MessagePersistencePort;
import com.tinder.chat.application.port.out.notification.ChatEventPort;
import com.tinder.chat.application.port.out.notification.ClientNotificationPort;
import com.tinder.chat.application.port.out.room.ChatParticipantPersistencePort;
import com.tinder.chat.application.service.room.ChatRoomValidator;
import com.tinder.chat.domain.enums.MessageContentType;
import com.tinder.chat.domain.enums.MessageStatus;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.shared.dto.media.MediaInitRequest;
import com.tinder.chat.shared.dto.media.MediaInitResponse;
import com.tinder.chat.shared.mapper.MessageEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageMediaService implements InitMediaUploadUseCase, ConfirmMediaUploadUseCase {

    private final MessagePersistencePort persistencePort;
    private final MessageOutboxEventPort outboxEventPort;
    private final ChatRoomValidator chatRoomValidator;
    private final ChatParticipantPersistencePort participantPersistencePort;
    private final MediaStoragePort storagePort;
    private final ChatEventPort eventPort;
    private final ClientNotificationPort notificationPort;
    private final MessageEventMapper messageEventMapper;

    @Override
    @Transactional
    public MediaInitResponse initMediaUpload(UUID chatId, UUID senderId, MediaInitRequest requestDto) {
        chatRoomValidator.validateAndGetParticipants(chatId, senderId);

        UUID fileId = UUID.randomUUID();
        String objectKey = buildObjectKey(chatId, fileId, requestDto.fileExtension());
        MessageContentType contentType = MessageContentType.valueOf(requestDto.type().toUpperCase());

        Message pendingMessage = Message.builder()
                .chatId(chatId)
                .senderId(senderId)
                .contentType(contentType)
                .content(objectKey)
                .status(MessageStatus.UPLOADING)
                .build();

        pendingMessage = persistencePort.save(pendingMessage);

        String uploadUrl = storagePort.generateTempLinkForUploading(objectKey);
        participantPersistencePort.updateLastReadMessageIdIfGreater(chatId, senderId, pendingMessage.getId());

        return new MediaInitResponse(requestDto.localId(), pendingMessage.getId(), uploadUrl);
    }

    @Override
    @Transactional
    public void confirmMediaUpload(String objectKey) {
        Message message = persistencePort.getPendingMessageByObjectKey(objectKey);

        if (message.getStatus() == MessageStatus.SENT) return;

        Set<UUID> participants = chatRoomValidator.validateAndGetParticipants(message.getChatId(), message.getSenderId());
        UUID recipientId = chatRoomValidator.getPartnerId(participants, message.getSenderId());

        message.markAsSent();
        Message confirmedMessage = persistencePort.save(message);
        outboxEventPort.publishMessageSavedEvent(confirmedMessage, recipientId);

        eventPort.publishNewMessage(messageEventMapper.toEventDto(confirmedMessage, recipientId));
        notificationPort.sendAck(message.getSenderId(), messageEventMapper.toAckDto(confirmedMessage, null));
    }

    private String buildObjectKey(UUID chatId, UUID fileId, String extension) {
        return String.format("chats/%s/%s%s", chatId, fileId, extension);
    }
}