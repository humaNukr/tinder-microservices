package com.tinder.auth.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ExternalAuthRequest(@NotBlank String token) {
}
