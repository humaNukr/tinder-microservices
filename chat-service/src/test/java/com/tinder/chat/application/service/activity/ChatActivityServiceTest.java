package com.tinder.chat.application.service.activity;

import com.tinder.chat.application.port.out.notification.ChatEventPort;
import com.tinder.chat.application.port.out.room.ChatParticipantPersistencePort;
import com.tinder.chat.application.service.room.ChatRoomValidator;
import com.tinder.chat.shared.dto.activity.ReadReceiptRequest;
import com.tinder.chat.shared.dto.event.ReadReceiptEventDto;
import com.tinder.chat.shared.dto.event.TypingEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatActivityServiceTest {

    @Mock
    private ChatParticipantPersistencePort persistencePort;
    @Mock
    private ChatRoomValidator chatRoomValidator;
    @Mock
    private ChatEventPort eventPort;

    @InjectMocks
    private ChatActivityService chatActivityService;

    @Captor
    private ArgumentCaptor<ReadReceiptEventDto> eventCaptor;

    private UUID chatId;
    private UUID readerId;
    private UUID partnerId;
    private Long messageId;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        readerId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
        messageId = 150L;
    }

    @Nested
    class ProcessReadReceipt {

        @Test
        void processReadReceipt_WatermarkUpdated_PublishesEvent() {
            ReadReceiptRequest request = new ReadReceiptRequest(chatId, messageId);

            when(persistencePort.updateLastReadMessageIdIfGreater(chatId, readerId, messageId)).thenReturn(1);
            setupValidator();

            chatActivityService.processReadReceipt(readerId, request);

            verify(persistencePort).updateLastReadMessageIdIfGreater(chatId, readerId, messageId);
            verify(chatRoomValidator).validateAndGetParticipants(chatId, readerId);
            verify(chatRoomValidator).getPartnerId(Set.of(readerId, partnerId), readerId);

            verify(eventPort).publishReadReceipt(eventCaptor.capture());
            ReadReceiptEventDto event = eventCaptor.getValue();
            assertEquals(chatId, event.chatId());
            assertEquals(readerId, event.readerId());
            assertEquals(partnerId, event.recipientId());
            assertEquals(messageId, event.messageId());
        }

        @Test
        void processReadReceipt_WatermarkNotUpdated_DoesNotPublishEvent() {
            ReadReceiptRequest request = new ReadReceiptRequest(chatId, messageId);

            when(persistencePort.updateLastReadMessageIdIfGreater(chatId, readerId, messageId)).thenReturn(0);

            chatActivityService.processReadReceipt(readerId, request);

            verify(persistencePort).updateLastReadMessageIdIfGreater(chatId, readerId, messageId);
            verifyNoInteractions(chatRoomValidator, eventPort);
        }
    }

    @Nested
    class ProcessTypingEvent {

        @Test
        void processTypingEvent_ValidRequest_PublishesTypingEvent() {
            TypingEventDto requestDto = new TypingEventDto(chatId, readerId);

            chatActivityService.processTypingEvent(requestDto);

            verify(eventPort).publishTypingEvent(requestDto);
            verifyNoInteractions(persistencePort, chatRoomValidator);
        }
    }

    private void setupValidator() {
        Set<UUID> participants = Set.of(readerId, partnerId);
        when(chatRoomValidator.validateAndGetParticipants(chatId, readerId)).thenReturn(participants);
        when(chatRoomValidator.getPartnerId(participants, readerId)).thenReturn(partnerId);
    }
}