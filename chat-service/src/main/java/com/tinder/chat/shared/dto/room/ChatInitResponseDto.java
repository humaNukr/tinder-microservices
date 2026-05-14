package com.tinder.chat.shared.dto.room;

import com.tinder.chat.shared.dto.message.MessageResponseDto;

import java.util.List;

public record ChatInitResponseDto(
        List<MessageResponseDto> messages,
        boolean isPartnerOnline,
        Long partnerLastReadMessageId,
        Long myLastReadMessageId,
        Long nextCursor,
        boolean hasNext
) {
}