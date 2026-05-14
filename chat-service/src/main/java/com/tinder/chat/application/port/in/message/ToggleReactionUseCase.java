package com.tinder.chat.application.port.in.message;

import com.tinder.chat.shared.dto.message.ReactionRequestDto;

import java.util.UUID;

public interface ToggleReactionUseCase {
    void toggleReaction(UUID senderId, ReactionRequestDto requestDto);
}