package com.tinder.chat.application.service.room;

import com.tinder.chat.application.port.out.room.ChatParticipantPort;
import com.tinder.chat.domain.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatRoomValidator {

    private final ChatParticipantPort participantProvider;

    public Set<UUID> validateAndGetParticipants(UUID chatId, UUID userId) {
        Set<UUID> participants = participantProvider.getParticipants(chatId);
        if (!participants.contains(userId)) {
            throw new AccessDeniedException("User is not a participant of this chat");
        }
        return participants;
    }

    public UUID getPartnerId(Set<UUID> participants, UUID userId) {
        return participants.stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Recipient not found"));
    }
}