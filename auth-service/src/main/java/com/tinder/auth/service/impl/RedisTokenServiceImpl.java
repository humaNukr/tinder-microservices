package com.tinder.auth.service.impl;

import com.tinder.auth.properties.JwtProperties;
import com.tinder.auth.properties.RedisAuthProperties;
import com.tinder.auth.service.interfaces.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenServiceImpl implements TokenService {

	private final StringRedisTemplate redisTemplate;
	private final JwtProperties jwtProperties;
	private final RedisAuthProperties redisAuthProperties;

	@Override
	public void storeRefreshToken(UUID userId, String deviceId, String token) {
		String key = buildSessionKey(userId);

		redisTemplate.opsForHash().put(key, deviceId, token);
		redisTemplate.expire(key, Duration.ofMillis(jwtProperties.refreshTokenExpirationMs()));

		log.debug("Stored refresh token for user {} on device {}", userId, deviceId);
	}

	@Override
	public String getRefreshToken(UUID userId, String deviceId) {
		String key = buildSessionKey(userId);
		Object token = redisTemplate.opsForHash().get(key, deviceId);

		return Optional.ofNullable(token).map(Object::toString).orElse(null);
	}

	@Override
	public void deleteRefreshToken(UUID userId, String deviceId) {
		String key = buildSessionKey(userId);
		redisTemplate.opsForHash().delete(key, deviceId);

		log.debug("Deleted refresh token for user {} on device {}", userId, deviceId);
	}

	@Override
	public void deleteAllUserTokens(UUID userId) {
		String key = buildSessionKey(userId);
		redisTemplate.delete(key);

		log.info("Deleted all sessions for user {}", userId);
	}

	private String buildSessionKey(UUID userId) {
		return redisAuthProperties.sessionPrefix() + userId + redisAuthProperties.sessionSuffix();
	}
}
