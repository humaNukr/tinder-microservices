package com.tinder.auth.dto.auth;

public record AuthResponse(String accessToken, String refreshToken, boolean isNewUser) {
}
