package com.tinder.chat.application.port.out.room;

import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantPersistencePort {
    int updateLastReadMessageIdIfGreater(UUID chatId, UUID userId, Long messageId);

    Optional<Long> findLastReadMessageId(UUID chatId, UUID userId);
}