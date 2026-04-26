package com.tinder.auth.service.impl;

import com.tinder.auth.properties.JwtProperties;
import com.tinder.auth.service.interfaces.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
	private final StringRedisTemplate stringRedisTemplate;
	private final JwtProperties jwtProperties;

	@Override
	public void storeRefreshTokenToRedis(UUID userId, String deviceId, String token) {
		String key = "user:" + userId + ":sessions";
		stringRedisTemplate.opsForHash().put(key, deviceId, token);
		stringRedisTemplate.expire(key, Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()));

	}

	@Override
	public String getRefreshTokenFromRedis(UUID userId, String deviceId) {
		Object token = stringRedisTemplate.opsForHash().get("user:" + userId + ":sessions", deviceId);

		return Optional.ofNullable(token).map(Object::toString).orElse(null);
	}

	@Override
	public void deleteRefreshTokenFromRedis(UUID userId, String deviceId) {
		String key = "user:" + userId + ":sessions";
		stringRedisTemplate.opsForHash().delete(key, deviceId);
	}

	@Override
	public void deleteAllUserTokensFromRedis(UUID userId) {
		String key = "user:" + userId + ":sessions";
		stringRedisTemplate.delete(key);
	}
}
