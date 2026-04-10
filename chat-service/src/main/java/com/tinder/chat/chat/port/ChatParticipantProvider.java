package com.tinder.chat.chat.port;

import java.util.Set;
import java.util.UUID;

public interface ChatParticipantProvider {
    void saveParticipants(UUID chatId, UUID user1Id, UUID user2Id);
    Set<UUID> getParticipants(UUID chatId);
}