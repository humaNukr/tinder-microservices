package com.tinder.chat.application.service.message;

import com.tinder.chat.application.port.out.message.MessagePersistencePort;
import com.tinder.chat.application.port.out.notification.ChatEventPort;
import com.tinder.chat.application.service.room.ChatRoomValidator;
import com.tinder.chat.domain.model.Message;
import com.tinder.chat.domain.model.MessageReaction;
import com.tinder.chat.shared.dto.event.ReactionEventDto;
import com.tinder.chat.shared.dto.message.ReactionRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageReactionServiceTest {

    @Mock
    private MessagePersistencePort persistencePort;
    @Mock
    private ChatRoomValidator chatRoomValidator;
    @Mock
    private ChatEventPort eventPort;

    @InjectMocks
    private MessageReactionService messageReactionService;

    @Captor
    private ArgumentCaptor<ReactionEventDto> eventCaptor;

    private UUID senderId;
    private UUID partnerId;
    private UUID chatId;
    private Long messageId;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
        chatId = UUID.randomUUID();
        messageId = 100L;
    }

    private Message createMessage(boolean isDeleted) {
        Message message = Message.builder()
                .id(messageId)
                .chatId(chatId)
                .senderId(senderId)
                .reactions(new ArrayList<>())
                .build();
        if (isDeleted) {
            message.deleteBy(senderId, chatId);
        }
        return message;
    }

    private void setupValidator(UUID targetChatId) {
        Set<UUID> participants = Set.of(senderId, partnerId);
        when(chatRoomValidator.validateAndGetParticipants(targetChatId, senderId)).thenReturn(participants);
        when(chatRoomValidator.getPartnerId(participants, senderId)).thenReturn(partnerId);
    }

    @Nested
    class ToggleReaction {

        @Test
        void toggleReaction_NewReaction_AddsReactionAndPublishesEvent() {
            ReactionRequestDto request = new ReactionRequestDto(chatId, messageId, "LIKE");
            Message message = createMessage(false);
            setupValidator(chatId);

            when(persistencePort.getByIdWithReactions(messageId)).thenReturn(message);

            messageReactionService.toggleReaction(senderId, request);

            verify(persistencePort).save(message);
            assertEquals(1, message.getReactions().size());
            assertEquals("LIKE", message.getReactions().getFirst().getReaction());

            verify(eventPort).publishReaction(eventCaptor.capture());
            ReactionEventDto event = eventCaptor.getValue();
            assertEquals("LIKE", event.reaction());
            assertFalse(event.isRemoved());
        }

        @Test
        void toggleReaction_DifferentExistingReaction_UpdatesReactionAndPublishesEvent() {
            ReactionRequestDto request = new ReactionRequestDto(chatId, messageId, "LOVE");
            Message message = createMessage(false);
            message.addReaction(MessageReaction.builder().userId(senderId).reaction("LIKE").build());
            setupValidator(chatId);

            when(persistencePort.getByIdWithReactions(messageId)).thenReturn(message);

            messageReactionService.toggleReaction(senderId, request);

            verify(persistencePort).save(message);
            assertEquals(1, message.getReactions().size());
            assertEquals("LOVE", message.getReactions().getFirst().getReaction());

            verify(eventPort).publishReaction(eventCaptor.capture());
            ReactionEventDto event = eventCaptor.getValue();
            assertEquals("LOVE", event.reaction());
            assertFalse(event.isRemoved());
        }

        @Test
        void toggleReaction_SameExistingReaction_RemovesReactionAndPublishesEvent() {
            ReactionRequestDto request = new ReactionRequestDto(chatId, messageId, "LIKE");
            Message message = createMessage(false);
            message.addReaction(MessageReaction.builder().userId(senderId).reaction("LIKE").build());
            setupValidator(chatId);

            when(persistencePort.getByIdWithReactions(messageId)).thenReturn(message);

            messageReactionService.toggleReaction(senderId, request);

            verify(persistencePort).save(message);
            assertTrue(message.getReactions().isEmpty());

            verify(eventPort).publishReaction(eventCaptor.capture());
            ReactionEventDto event = eventCaptor.getValue();
            assertTrue(event.isRemoved());
        }

        @Test
        void toggleReaction_WrongChatId_ThrowsIllegalArgumentException() {
            UUID wrongChatId = UUID.randomUUID();
            ReactionRequestDto request = new ReactionRequestDto(wrongChatId, messageId, "LIKE");
            Message message = createMessage(false);

            setupValidator(wrongChatId);
            when(persistencePort.getByIdWithReactions(messageId)).thenReturn(message);

            assertThrows(IllegalArgumentException.class, () ->
                    messageReactionService.toggleReaction(senderId, request)
            );
        }

        @Test
        void toggleReaction_MessageDeleted_ThrowsIllegalStateException() {
            ReactionRequestDto request = new ReactionRequestDto(chatId, messageId, "LIKE");
            Message message = createMessage(true);
            setupValidator(chatId);

            when(persistencePort.getByIdWithReactions(messageId)).thenReturn(message);

            assertThrows(IllegalStateException.class, () ->
                    messageReactionService.toggleReaction(senderId, request)
            );
        }
    }
}