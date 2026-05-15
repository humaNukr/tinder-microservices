package com.tinder.chat.infrastructure.adapter.in.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.chat.application.port.out.notification.ClientNotificationPort;
import com.tinder.chat.shared.dto.event.MessageDeletedEventDto;
import com.tinder.chat.shared.dto.event.MessageEditedEventDto;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import com.tinder.chat.shared.dto.event.ReactionEventDto;
import com.tinder.chat.shared.dto.event.ReadReceiptEventDto;
import com.tinder.chat.shared.dto.event.TypingEventDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatSubscriberAdapterTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ClientNotificationPort notificationPort;

    @InjectMocks
    private RedisChatSubscriberAdapter adapter;

    @Nested
    class EventHandlers {

        @Test
        void handleChatMessage_ValidJson_SendsNewMessage() throws JsonProcessingException {
            String json = "{}";
            MessageEventDto dto = mock(MessageEventDto.class);
            when(objectMapper.readValue(json, MessageEventDto.class)).thenReturn(dto);

            adapter.handleChatMessage(json);

            verify(notificationPort).sendNewMessage(dto);
        }

        @Test
        void handleTypingEvent_ValidJson_SendsTypingEvent() throws JsonProcessingException {
            String json = "{}";
            TypingEventDto dto = mock(TypingEventDto.class);
            when(objectMapper.readValue(json, TypingEventDto.class)).thenReturn(dto);

            adapter.handleTypingEvent(json);

            verify(notificationPort).sendTypingEvent(dto);
        }

        @Test
        void handleReadReceipt_ValidJson_SendsReadReceipt() throws JsonProcessingException {
            String json = "{}";
            ReadReceiptEventDto dto = mock(ReadReceiptEventDto.class);
            when(objectMapper.readValue(json, ReadReceiptEventDto.class)).thenReturn(dto);

            adapter.handleReadReceipt(json);

            verify(notificationPort).sendReadReceipt(dto);
        }

        @Test
        void handleReactionEvent_ValidJson_SendsReaction() throws JsonProcessingException {
            String json = "{}";
            ReactionEventDto dto = mock(ReactionEventDto.class);
            when(objectMapper.readValue(json, ReactionEventDto.class)).thenReturn(dto);

            adapter.handleReactionEvent(json);

            verify(notificationPort).sendReaction(dto);
        }

        @Test
        void handleMessageDeletedEvent_ValidJson_SendsMessageDeleted() throws JsonProcessingException {
            String json = "{}";
            MessageDeletedEventDto dto = mock(MessageDeletedEventDto.class);
            when(objectMapper.readValue(json, MessageDeletedEventDto.class)).thenReturn(dto);

            adapter.handleMessageDeletedEvent(json);

            verify(notificationPort).sendMessageDeleted(dto);
        }

        @Test
        void handleMessageEditedEvent_ValidJson_SendsMessageEdited() throws JsonProcessingException {
            String json = "{}";
            MessageEditedEventDto dto = mock(MessageEditedEventDto.class);
            when(objectMapper.readValue(json, MessageEditedEventDto.class)).thenReturn(dto);

            adapter.handleMessageEditedEvent(json);

            verify(notificationPort).sendMessageEdited(dto);
        }
    }
}