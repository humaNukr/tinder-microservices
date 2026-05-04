package com.tinder.chat.chat.service;

import java.util.UUID;

public interface ChatParticipantService {
    Long getParticipantWatermark(UUID chatId, UUID userId);

    int updateWatermark(UUID chatId, UUID userId, Long messageId);
}
