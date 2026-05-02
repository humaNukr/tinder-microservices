package com.tinder.chat.chat.dto;

import com.tinder.chat.message.dto.MessageResponseDto;

import java.util.List;

public record ChatHistoryResponseDto(
        List<MessageResponseDto> messages,
        Long nextCursor,
        boolean hasNext
) {
}