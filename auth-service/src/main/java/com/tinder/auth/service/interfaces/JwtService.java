package com.tinder.auth.service.interfaces;

import com.tinder.auth.entity.User;

public interface JwtService {
	String generateAccessToken(User user);

	String generateRefreshToken(User user);

	String extractUserId(String token);
}
