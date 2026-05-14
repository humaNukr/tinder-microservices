package com.tinder.chat.application.port.out.room;

import java.util.Set;
import java.util.UUID;

public interface ChatParticipantPort {
    void saveParticipants(UUID chatId, UUID user1Id, UUID user2Id);

    Set<UUID> getParticipants(UUID chatId);
}