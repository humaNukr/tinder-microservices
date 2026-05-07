package com.tinder.chat.chat.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface ChatPreviewProjection {
    UUID getChatId();
    UUID getPartnerId();
    String getLastMessageContent();
    String getLastMessageType();
    Instant getLastMessageCreatedAt();
    UUID getLastMessageSenderId();
    Integer getUnreadCount();
}