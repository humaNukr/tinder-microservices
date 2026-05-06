package com.tinder.chat.chat.dto;

import com.tinder.chat.message.dto.MessageResponseDto;

import java.util.List;

public record ChatInitResponseDto(
        List<MessageResponseDto> messages,
        boolean isPartnerOnline,
        Long partnerLastReadMessageId,
        Long myLastReadMessageId,
        Long nextCursor,
        boolean hasNext
) {}