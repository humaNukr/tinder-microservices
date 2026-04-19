package com.tinder.profile.dto;

import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest(
        @NotNull Double longitude,
        @NotNull Double latitude
) {
}
