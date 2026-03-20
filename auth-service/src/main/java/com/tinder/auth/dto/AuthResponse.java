package com.tinder.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, boolean isNewUser) {
}
