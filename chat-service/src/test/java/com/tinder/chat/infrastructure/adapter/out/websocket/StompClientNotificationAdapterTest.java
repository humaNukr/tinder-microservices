package com.tinder.chat.infrastructure.adapter.out.websocket;

import com.tinder.chat.infrastructure.config.properties.WebSocketProperties;
import com.tinder.chat.shared.dto.event.MessageAckDto;
import com.tinder.chat.shared.dto.event.MessageDeletedEventDto;
import com.tinder.chat.shared.dto.event.MessageEventDto;
import com.tinder.chat.shared.dto.event.TypingEventDto;
import com.tinder.chat.shared.dto.event.UserPresenceEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompClientNotificationAdapterTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketProperties webSocketProperties;

    @InjectMocks
    private StompClientNotificationAdapter adapter;

    @Nested
    class DirectUserRouting {

        @Test
        void sendAck_SendsToSpecificUserQueue() {
            UUID userId = UUID.randomUUID();
            MessageAckDto dto = mock(MessageAckDto.class);
            when(webSocketProperties.queueAcks()).thenReturn("/queue/acks");

            adapter.sendAck(userId, dto);

            verify(messagingTemplate).convertAndSendToUser(userId.toString(), "/queue/acks", dto);
        }

        @Test
        void sendNewMessage_SendsToRecipientQueue() {
            UUID recipientId = UUID.randomUUID();
            MessageEventDto dto = mock(MessageEventDto.class);
            when(dto.recipientId()).thenReturn(recipientId);
            when(webSocketProperties.queueMessages()).thenReturn("/queue/messages");

            adapter.sendNewMessage(dto);

            verify(messagingTemplate).convertAndSendToUser(recipientId.toString(), "/queue/messages", dto);
        }
    }

    @Nested
    class DualUserRouting {

        @Test
        void sendMessageDeleted_SendsToBothSenderAndRecipient() {
            UUID senderId = UUID.randomUUID();
            UUID recipientId = UUID.randomUUID();
            MessageDeletedEventDto dto = mock(MessageDeletedEventDto.class);

            when(dto.senderId()).thenReturn(senderId);
            when(dto.recipientId()).thenReturn(recipientId);
            when(webSocketProperties.queueMessageDeletions()).thenReturn("/queue/deletions");

            adapter.sendMessageDeleted(dto);

            // Перевіряємо, що відправили обом!
            verify(messagingTemplate).convertAndSendToUser(recipientId.toString(), "/queue/deletions", dto);
            verify(messagingTemplate).convertAndSendToUser(senderId.toString(), "/queue/deletions", dto);
        }
    }

    @Nested
    class TopicBroadcasting {

        @Test
        void sendTypingEvent_ConstructsCorrectDestinationAndSends() {
            UUID chatId = UUID.randomUUID();
            TypingEventDto dto = mock(TypingEventDto.class);

            when(dto.chatId()).thenReturn(chatId);
            when(webSocketProperties.topicChatPrefix()).thenReturn("/topic/chat");
            when(webSocketProperties.topicChatTypingSuffix()).thenReturn("typing");

            adapter.sendTypingEvent(dto);

            String expectedDestination = "/topic/chat/" + chatId + "/typing";
            verify(messagingTemplate).convertAndSend(expectedDestination, dto);
        }

        @Test
        void sendPresenceEvent_ConstructsCorrectDestinationAndSends() {
            UUID userId = UUID.randomUUID();
            UserPresenceEvent event = mock(UserPresenceEvent.class);

            when(event.userId()).thenReturn(userId);
            when(webSocketProperties.topicUsersPrefix()).thenReturn("/topic/users");
            when(webSocketProperties.topicUsersPresenceSuffix()).thenReturn("presence");

            adapter.sendPresenceEvent(event);

            String expectedDestination = "/topic/users/" + userId + "/presence";
            verify(messagingTemplate).convertAndSend(expectedDestination, event);
        }
    }
}