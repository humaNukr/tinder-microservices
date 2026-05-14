package com.tinder.chat.shared.dto.room;

import com.tinder.chat.shared.dto.message.MessageResponseDto;

import java.util.List;

public record ChatHistoryResponseDto(
        List<MessageResponseDto> messages,
        Long nextCursor,
        boolean hasNext
) {
}