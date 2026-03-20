package com.tinder.auth.service.impl;

import com.tinder.auth.service.interfaces.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
	private final StringRedisTemplate stringRedisTemplate;

	@Override
	public void storeRefreshTokenToRedis(UUID userId, String token) {
		stringRedisTemplate.opsForValue().set("refresh_token:" + userId, token, Duration.ofDays(30));
	}

	@Override
	public String getRefreshTokenFromRedis(UUID userId) {
		return stringRedisTemplate.opsForValue().get("refresh_token:" + userId);
	}
}
