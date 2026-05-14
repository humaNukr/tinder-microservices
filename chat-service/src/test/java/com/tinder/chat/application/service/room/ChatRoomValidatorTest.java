package com.tinder.chat.application.service.room;

import com.tinder.chat.application.port.out.room.ChatParticipantPort;
import com.tinder.chat.domain.exception.AccessDeniedException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomValidatorTest {

    @Mock
    private ChatParticipantPort participantProvider;

    @InjectMocks
    private ChatRoomValidator chatRoomValidator;

    private UUID chatId;
    private UUID currentUserId;
    private UUID partnerId;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        currentUserId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
    }

    @Nested
    class ValidateAndGetParticipants {

        @Test
        void validateAndGetParticipants_UserIsParticipant_ReturnsParticipants() {
            Set<UUID> participants = createParticipantsSet(currentUserId, partnerId);
            when(participantProvider.getParticipants(chatId)).thenReturn(participants);

            Set<UUID> result = chatRoomValidator.validateAndGetParticipants(chatId, currentUserId);

            assertEquals(participants, result);
            assertTrue(result.contains(currentUserId));
            verify(participantProvider).getParticipants(chatId);
        }

        @Test
        void validateAndGetParticipants_UserIsNotParticipant_ThrowsAccessDeniedException() {
            Set<UUID> participants = createParticipantsSet(UUID.randomUUID(), UUID.randomUUID());
            when(participantProvider.getParticipants(chatId)).thenReturn(participants);

            assertThrows(AccessDeniedException.class, () ->
                    chatRoomValidator.validateAndGetParticipants(chatId, currentUserId)
            );
            verify(participantProvider).getParticipants(chatId);
        }
    }

    @Nested
    class GetPartnerId {

        @Test
        void getPartnerId_ValidParticipantsProvided_ReturnsPartnerId() {
            Set<UUID> participants = createParticipantsSet(currentUserId, partnerId);

            UUID result = chatRoomValidator.getPartnerId(participants, currentUserId);

            assertEquals(partnerId, result);
        }

        @Test
        void getPartnerId_PartnerNotFound_ThrowsIllegalStateException() {
            Set<UUID> participants = Set.of(currentUserId);

            assertThrows(IllegalStateException.class, () ->
                    chatRoomValidator.getPartnerId(participants, currentUserId)
            );
        }
    }

    private Set<UUID> createParticipantsSet(UUID user1, UUID user2) {
        return Set.of(user1, user2);
    }
}