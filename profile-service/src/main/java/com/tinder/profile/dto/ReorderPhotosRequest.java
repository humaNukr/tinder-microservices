package com.tinder.profile.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderPhotosRequest(
        @NotNull List<String> photoUrls
) {
}