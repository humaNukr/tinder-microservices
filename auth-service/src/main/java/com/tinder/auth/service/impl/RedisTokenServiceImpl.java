package com.tinder.auth.service.impl;

import com.tinder.auth.properties.JwtProperties;
import com.tinder.auth.properties.RedisAuthProperties;
import com.tinder.auth.service.interfaces.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
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
	@SuppressWarnings("unchecked")
	public void storeRefreshToken(UUID userId, String deviceId, String token) {
		String key = buildSessionKey(userId);
		long expirationMs = jwtProperties.refreshTokenExpirationMs();

		redisTemplate.execute(new SessionCallback<List<Object>>() {
			@Override
			public List<Object> execute(@NonNull RedisOperations operations) throws DataAccessException {
				operations.multi();
				operations.opsForHash().put(key, deviceId, token);
				operations.expire(key, Duration.ofMillis(expirationMs));
				return operations.exec();
			}
		});

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
