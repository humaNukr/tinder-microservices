package com.tinder.chat.application.service.message;

import com.tinder.chat.application.port.out.common.IdempotencyPort;
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
import com.tinder.chat.shared.dto.event.MessageDeletedEventDto;
import com.tinder.chat.shared.dto.event.MessageEditedEventDto;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import com.tinder.chat.shared.dto.message.ChatRequestDto;
import com.tinder.chat.shared.dto.message.EditMessageRequest;
import com.tinder.chat.shared.dto.message.MessageDeleteDto;
import com.tinder.chat.shared.mapper.MessageEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagePublishingServiceTest {

    @Mock
    private MessagePersistencePort persistencePort;
    @Mock
    private MessageOutboxEventPort outboxEventPort;
    @Mock
    private ChatRoomValidator chatRoomValidator;
    @Mock
    private ChatParticipantPersistencePort participantPersistencePort;
    @Mock
    private ChatEventPort eventPort;
    @Mock
    private ClientNotificationPort notificationPort;
    @Mock
    private IdempotencyPort idempotencyPort;
    @Mock
    private MessageEventMapper messageEventMapper;

    @InjectMocks
    private MessagePublishingService messagePublishingService;

    private UUID chatId;
    private UUID senderId;
    private UUID partnerId;
    private Long messageId;
    private UUID localId;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
        messageId = 1L;
        localId = UUID.randomUUID();
    }

    @Nested
    class SaveMessage {

        @Test
        void saveMessage_ValidRequest_SavesAndPublishesEvent() {
            ChatRequestDto requestDto = createChatRequestDto(null);
            Message savedMessage = createMessage();
            MessageEventDto eventDto = mock(MessageEventDto.class);
            MessageAckDto ackDto = mock(MessageAckDto.class);

            when(idempotencyPort.tryAcquire(anyString(), any())).thenReturn(true);
            setupChatRoomValidator();
            when(persistencePort.save(any(Message.class))).thenReturn(savedMessage);
            when(messageEventMapper.toEventDto(savedMessage, partnerId)).thenReturn(eventDto);
            when(messageEventMapper.toAckDto(savedMessage, localId)).thenReturn(ackDto);

            messagePublishingService.saveMessage(senderId, requestDto);

            verify(persistencePort).save(any(Message.class));
            verify(outboxEventPort).publishMessageSavedEvent(savedMessage, partnerId);
            verify(participantPersistencePort).updateLastReadMessageIdIfGreater(chatId, senderId, messageId);
            verify(eventPort).publishNewMessage(eventDto);
            verify(idempotencyPort).complete(anyString(), eq(messageId.toString()), any());
            verify(notificationPort).sendAck(senderId, ackDto);
        }

        @Test
        void saveMessage_IdempotencyCollision_SendsAckWithoutSaving() {
            ChatRequestDto requestDto = createChatRequestDto(null);
            Message existingMessage = createMessage();

            when(idempotencyPort.tryAcquire(anyString(), any())).thenReturn(false);
            when(idempotencyPort.getResult(anyString())).thenReturn("123");
            when(persistencePort.getById(123L)).thenReturn(existingMessage);

            messagePublishingService.saveMessage(senderId, requestDto);

            verify(persistencePort, never()).save(any());
            verify(eventPort, never()).publishNewMessage(any());
            verify(notificationPort).sendAck(eq(senderId), any(MessageAckDto.class));
        }

        @Test
        void saveMessage_ParentMessageFromDifferentChat_ThrowsException() {
            UUID invalidChatId = UUID.randomUUID();
            Message parentMessage = Message.builder().chatId(invalidChatId).build();
            ChatRequestDto requestDto = createChatRequestDto(1L);

            when(idempotencyPort.tryAcquire(anyString(), any())).thenReturn(true);
            setupChatRoomValidator();
            when(persistencePort.getById(1L)).thenReturn(parentMessage);

            assertThrows(IllegalArgumentException.class, () ->
                    messagePublishingService.saveMessage(senderId, requestDto)
            );

            verify(persistencePort, never()).save(any());
        }
    }

    @Nested
    class EditMessage {

        @Test
        void editMessage_ValidRequest_EditsAndPublishesEvent() {
            EditMessageRequest requestDto = new EditMessageRequest(messageId, chatId, "Edited content");
            Message message = createMessage();
            MessageEditedEventDto eventDto = mock(MessageEditedEventDto.class);

            when(persistencePort.getById(messageId)).thenReturn(message);
            setupChatRoomValidator();
            when(persistencePort.save(message)).thenReturn(message);
            when(messageEventMapper.toEditedEventDto(message, partnerId)).thenReturn(eventDto);

            messagePublishingService.editMessage(senderId, requestDto);

            verify(persistencePort).save(message);
            verify(eventPort).publishMessageEdited(eventDto);
        }
    }

    @Nested
    class DeleteMessage {

        @Test
        void deleteMessage_ValidRequest_DeletesAndPublishesEvent() {
            MessageDeleteDto requestDto = new MessageDeleteDto(chatId, messageId);
            Message message = createMessage();
            MessageDeletedEventDto eventDto = mock(MessageDeletedEventDto.class);

            setupChatRoomValidator();
            when(persistencePort.getById(messageId)).thenReturn(message);
            when(persistencePort.save(message)).thenReturn(message);
            when(messageEventMapper.toDeletedEventDto(message, partnerId)).thenReturn(eventDto);

            messagePublishingService.deleteMessage(senderId, requestDto);

            verify(persistencePort).save(message);
            verify(eventPort).publishMessageDeleted(eventDto);
        }
    }

    private ChatRequestDto createChatRequestDto(Long replyToMessageId) {
        return new ChatRequestDto(
                localId,
                chatId,
                "TEXT",
                MessageContentType.TEXT.name(),
                replyToMessageId
        );
    }

    private Message createMessage() {
        return Message.builder()
                .id(1L)
                .chatId(chatId)
                .senderId(senderId)
                .content("Original content")
                .contentType(MessageContentType.TEXT)
                .status(MessageStatus.SENT)
                .createdAt(Instant.now())
                .build();
    }

    private void setupChatRoomValidator() {
        Set<UUID> participants = Set.of(senderId, partnerId);
        when(chatRoomValidator.validateAndGetParticipants(chatId, senderId)).thenReturn(participants);
        when(chatRoomValidator.getPartnerId(participants, senderId)).thenReturn(partnerId);
    }
}