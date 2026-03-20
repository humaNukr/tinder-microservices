package com.tinder.auth.service.interfaces;

import java.util.UUID;

public interface TokenService {
	void storeRefreshTokenToRedis(UUID userId, String token);

	String getRefreshTokenFromRedis(UUID userId);
}
