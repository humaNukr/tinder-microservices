package com.tinder.swipe.dto.swipe;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwipeRequestDto(
        @NotNull UUID targetId,
        @NotNull Boolean isLiked
) {
}
