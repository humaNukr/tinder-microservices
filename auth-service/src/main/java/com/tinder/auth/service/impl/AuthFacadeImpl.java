package com.tinder.auth.service.impl;

import com.tinder.auth.dto.auth.AuthResponse;
import com.tinder.auth.dto.otp.DeliveryChannel;
import com.tinder.auth.dto.user.UserResult;
import com.tinder.auth.entity.User;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.exception.AuthenticationFailedException;
import com.tinder.auth.publisher.UserActivityPublisher;
import com.tinder.auth.service.interfaces.AuthFacade;
import com.tinder.auth.service.interfaces.ExternalTokenVerifier;
import com.tinder.auth.service.interfaces.JwtService;
import com.tinder.auth.service.interfaces.OtpService;
import com.tinder.auth.service.interfaces.TokenService;
import com.tinder.auth.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

	private final OtpService otpService;
	private final TokenService tokenService;
	private final JwtService jwtService;
	private final UserService userService;
	private final UserActivityPublisher activityPublisher;
	private final List<ExternalTokenVerifier> externalVerifiers;

	@Override
	public void sendOtp(String identifier, DeliveryChannel channel) {
		log.info("Initiating OTP sending for identifier: {}", identifier);
		otpService.generateAndSendOtp(identifier, channel);
	}

	@Override
	public AuthResponse verifyAndAuthenticate(String email, String deviceId, String code) {
		log.debug("Verifying OTP for email: {} from device: {}", email, deviceId);

		if (!otpService.validateOtp(email, code)) {
			log.warn("Failed OTP verification for email: {}", email);
			throw new AuthenticationFailedException("Invalid OTP");
		}

		return processAuthentication(email, deviceId, User.AuthProvider.EMAIL_OTP);
	}

	@Override
	public AuthResponse refreshToken(String requestRefreshToken, String deviceId) {
		String userIdStr = jwtService.extractUserId(requestRefreshToken);
		UUID userId = UUID.fromString(userIdStr);

		log.debug("Attempting to refresh token for user: {} on device: {}", userId, deviceId);

		String savedToken = tokenService.getRefreshToken(userId, deviceId);
		if (savedToken == null || !savedToken.equals(requestRefreshToken)) {
			log.warn("Token mismatch or missing in storage for user: {}, device: {}", userId, deviceId);
			throw new AuthenticationFailedException("Invalid or revoked refresh token");
		}

		User user = userService.findUserById(userId);

		String newAccessToken = jwtService.generateAccessToken(user);
		String newRefreshToken = jwtService.generateRefreshToken(user);

		tokenService.storeRefreshToken(userId, deviceId, newRefreshToken);
		activityPublisher.publishActivity(userId, ActivityType.TOKEN_REFRESH);

		log.info("Successfully refreshed tokens for user: {} on device: {}", userId, deviceId);
		return new AuthResponse(newAccessToken, newRefreshToken, false);
	}

	@Override
	public AuthResponse authenticateWithExternalProvider(String externalToken, String deviceId,
			User.AuthProvider provider) {
		log.debug("Authenticating via {} from device: {}", provider, deviceId);

		ExternalTokenVerifier verifier = externalVerifiers.stream().filter(v -> v.getSupportedProvider() == provider)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported auth provider: " + provider));

		String identifier = verifier.verifyTokenAndGetIdentifier(externalToken);

		return processAuthentication(identifier, deviceId, provider);
	}

	@Override
	public void logout(UUID userId, String deviceId) {
		log.info("Logging out user: {} from device: {}", userId, deviceId);
		tokenService.deleteRefreshToken(userId, deviceId);
	}

	@Override
	public void deleteAccount(UUID userId) {
		log.info("Deleting account and revoking all tokens for user: {}", userId);
		tokenService.deleteAllUserTokens(userId);
		userService.deleteUser(userId);
	}

	private AuthResponse processAuthentication(String identifier, String deviceId, User.AuthProvider provider) {
		UserResult userResult = userService.findOrCreateUser(identifier, provider);
		UUID userId = userResult.user().getId();

		String accessToken = jwtService.generateAccessToken(userResult.user());
		String refreshToken = jwtService.generateRefreshToken(userResult.user());

		tokenService.storeRefreshToken(userId, deviceId, refreshToken);
		activityPublisher.publishActivity(userId, ActivityType.LOGIN);

		log.info("User {} successfully authenticated. isNew: {}, device: {}", userId, userResult.isNew(), deviceId);
		return new AuthResponse(accessToken, refreshToken, userResult.isNew());
	}
}
