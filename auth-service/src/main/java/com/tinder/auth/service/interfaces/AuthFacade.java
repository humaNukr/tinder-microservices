package com.tinder.auth.service.interfaces;

import com.tinder.auth.dto.auth.AuthResponse;
import com.tinder.auth.dto.otp.DeliveryChannel;

import java.util.UUID;

public interface AuthFacade {

	void sendOtp(String identifier, DeliveryChannel channel);

	AuthResponse verifyAndAuthenticate(String email, String deviceId, String code);

	AuthResponse refreshToken(String refreshToken, String deviceId);

	AuthResponse authenticateWithGoogle(String idToken, String deviceId);

	void deleteAccount(UUID userId);

	void logout(UUID userId, String deviceId);
}
