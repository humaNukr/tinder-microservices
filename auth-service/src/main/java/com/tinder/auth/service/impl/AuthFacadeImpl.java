package com.tinder.auth.service.impl;

import com.tinder.auth.dto.AuthResponse;
import com.tinder.auth.dto.user.UserResult;
import com.tinder.auth.entity.User;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.producer.UserActivityProducer;
import com.tinder.auth.service.interfaces.AuthFacade;
import com.tinder.auth.service.interfaces.JwtService;
import com.tinder.auth.service.interfaces.OtpService;
import com.tinder.auth.service.interfaces.TokenService;
import com.tinder.auth.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {
	private final OtpService otpService;
	private final TokenService tokenService;
	private final JwtService jwtService;
	private final UserService userService;
	private final UserActivityProducer activityProducer;

	public void sendOtp(String identifier) {
		otpService.generateAndSendOtp(identifier);
	}

	@Override
	public AuthResponse verifyAndAuthenticate(String email, String code) {
		if (!otpService.validateOtp(email, code)) {
			throw new BadCredentialsException("Invalid OTP");
		}

		UserResult userResult = userService.findOrCreateUser(email);

		String accessToken = jwtService.generateAccessToken(userResult.user());
		String refreshToken = jwtService.generateRefreshToken(userResult.user());

		tokenService.storeRefreshTokenToRedis(userResult.user().getId(), refreshToken);

		activityProducer.publishActivity(userResult.user().getId(), ActivityType.LOGIN);

		return new AuthResponse(accessToken, refreshToken, userResult.isNew());
	}

	@Override
	public AuthResponse refreshToken(String requestRefreshToken) {
		String userIdStr = jwtService.extractUserId(requestRefreshToken);
		UUID userId = UUID.fromString(userIdStr);

		String savedToken = tokenService.getRefreshTokenFromRedis(userId);
		if (savedToken == null || !savedToken.equals(requestRefreshToken)) {
			throw new BadCredentialsException("Invalid or revoked refresh token");
		}

		User user = userService.findUserById(userId);

		String newAccessToken = jwtService.generateAccessToken(user);
		String newRefreshToken = jwtService.generateRefreshToken(user);

		tokenService.storeRefreshTokenToRedis(userId, newRefreshToken);

		activityProducer.publishActivity(userId, ActivityType.TOKEN_REFRESH);

		return new AuthResponse(newAccessToken, newRefreshToken, false);
	}
}
