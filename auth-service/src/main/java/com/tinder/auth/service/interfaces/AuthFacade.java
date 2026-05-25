package com.tinder.auth.service.interfaces;

import com.tinder.auth.dto.auth.AuthResponse;
import com.tinder.auth.dto.otp.DeliveryChannel;
import com.tinder.auth.entity.User;

import java.util.UUID;

public interface AuthFacade {

	void sendOtp(String identifier, DeliveryChannel channel);

	AuthResponse verifyAndAuthenticate(String identifier, String deviceId, String code);

	AuthResponse refreshToken(String refreshToken, String deviceId);

	AuthResponse authenticateWithExternalProvider(String externalToken, String deviceId, User.AuthProvider provider);

	void deleteAccount(UUID userId);

	void logout(UUID userId, String deviceId);
}
