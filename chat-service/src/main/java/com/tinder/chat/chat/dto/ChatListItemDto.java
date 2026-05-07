package com.tinder.chat.chat.dto;

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
) {}