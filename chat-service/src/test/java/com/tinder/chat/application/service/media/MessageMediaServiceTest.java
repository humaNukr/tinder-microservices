package com.tinder.chat.application.service.media;

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
import com.tinder.chat.shared.dto.event.MessageAckDto;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import com.tinder.chat.shared.dto.media.MediaInitRequest;
import com.tinder.chat.shared.dto.media.MediaInitResponse;
import com.tinder.chat.shared.mapper.MessageEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageMediaServiceTest {

    @Mock
    private MessagePersistencePort persistencePort;
    @Mock
    private MessageOutboxEventPort outboxEventPort;
    @Mock
    private ChatRoomValidator chatRoomValidator;
    @Mock
    private ChatParticipantPersistencePort participantPersistencePort;
    @Mock
    private MediaStoragePort storagePort;
    @Mock
    private ChatEventPort eventPort;
    @Mock
    private ClientNotificationPort notificationPort;
    @Mock
    private MessageEventMapper messageEventMapper;

    @InjectMocks
    private MessageMediaService messageMediaService;

    private UUID chatId;
    private UUID senderId;
    private UUID partnerId;
    private Long messageId;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
        messageId = 1L;
    }

    private Message createMessage(MessageStatus status) {
        return Message.builder()
                .id(messageId)
                .chatId(chatId)
                .senderId(senderId)
                .contentType(MessageContentType.IMAGE)
                .content("some-object-key")
                .status(status)
                .build();
    }

    private void setupParticipantValidation() {
        when(chatRoomValidator.validateAndGetParticipants(chatId, senderId)).thenReturn(Set.of(senderId, partnerId));
    }

    private void setupPartnerValidation() {
        Set<UUID> participants = Set.of(senderId, partnerId);
        when(chatRoomValidator.validateAndGetParticipants(chatId, senderId)).thenReturn(participants);
        when(chatRoomValidator.getPartnerId(participants, senderId)).thenReturn(partnerId);
    }

    @Nested
    class InitMediaUpload {

        @Test
        void initMediaUpload_ValidRequest_SavesMessageAndReturnsResponse() {
            MediaInitRequest requestDto = mock(MediaInitRequest.class);
            UUID localId = UUID.randomUUID();
            String uploadUrl = "https://storage.com/upload-url";
            Message savedMessage = createMessage(MessageStatus.UPLOADING);

            setupParticipantValidation();
            when(requestDto.fileExtension()).thenReturn(".jpg");
            when(requestDto.type()).thenReturn("IMAGE");
            when(requestDto.localId()).thenReturn(localId);
            when(persistencePort.save(any(Message.class))).thenReturn(savedMessage);
            when(storagePort.generateTempLinkForUploading(anyString())).thenReturn(uploadUrl);

            MediaInitResponse response = messageMediaService.initMediaUpload(chatId, senderId, requestDto);

            assertNotNull(response);
            assertEquals(localId, response.localId());
            assertEquals(messageId, response.dbId());
            assertEquals(uploadUrl, response.uploadUrl());

            verify(persistencePort).save(any(Message.class));
            verify(participantPersistencePort).updateLastReadMessageIdIfGreater(chatId, senderId, messageId);
        }
    }

    @Nested
    class ConfirmMediaUpload {

        @Test
        void confirmMediaUpload_MessageStatusUploading_ConfirmsAndPublishesEvents() {
            String objectKey = "chats/chat-id/file-id.jpg";
            Message pendingMessage = createMessage(MessageStatus.UPLOADING);
            Message confirmedMessage = createMessage(MessageStatus.SENT);
            MessageEventDto eventDto = mock(MessageEventDto.class);
            MessageAckDto ackDto = mock(MessageAckDto.class);

            when(persistencePort.getPendingMessageByObjectKey(objectKey)).thenReturn(pendingMessage);
            setupPartnerValidation();
            when(persistencePort.save(pendingMessage)).thenReturn(confirmedMessage);
            when(messageEventMapper.toEventDto(confirmedMessage, partnerId)).thenReturn(eventDto);
            when(messageEventMapper.toAckDto(confirmedMessage, null)).thenReturn(ackDto);

            messageMediaService.confirmMediaUpload(objectKey);

            verify(persistencePort).save(pendingMessage);
            verify(outboxEventPort).publishMessageSavedEvent(confirmedMessage, partnerId);
            verify(eventPort).publishNewMessage(eventDto);
            verify(notificationPort).sendAck(senderId, ackDto);
        }

        @Test
        void confirmMediaUpload_MessageStatusSent_ReturnsEarly() {
            String objectKey = "chats/chat-id/file-id.jpg";
            Message sentMessage = createMessage(MessageStatus.SENT);

            when(persistencePort.getPendingMessageByObjectKey(objectKey)).thenReturn(sentMessage);

            messageMediaService.confirmMediaUpload(objectKey);

            verify(persistencePort, never()).save(any(Message.class));
            verifyNoInteractions(chatRoomValidator, outboxEventPort, eventPort, notificationPort);
        }
    }
}