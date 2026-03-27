package com.tinder.swipe.dto.swipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwipeRequestDto(
        @NotBlank UUID targetId,
        @NotNull Boolean isLiked
) {
}
