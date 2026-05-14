package com.tinder.chat.shared.dto.room;

import java.time.Instant;
import java.util.UUID;

public record ChatListItemDto(
        UUID chatId,
        UUID partnerId,
        String partnerName,
        String partnerAvatarUrl,
        boolean isPartnerOnline,
        String lastMessageContent,
        String lastMessageType,
        Instant lastMessageAt,
        boolean isLastMessageMine,
        int unreadCount
) {
}