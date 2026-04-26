package com.tinder.auth.controller;

import com.tinder.auth.dto.AuthResponse;
import com.tinder.auth.dto.google.GoogleAuthRequest;
import com.tinder.auth.dto.jwt.RefreshTokenDto;
import com.tinder.auth.dto.otp.SendOtpRequest;
import com.tinder.auth.dto.otp.VerifyOtpRequest;
import com.tinder.auth.service.interfaces.AuthFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {
	private final AuthFacade authFacade;

	@PostMapping("/send-otp")
	@ResponseStatus(HttpStatus.OK)
	public void sendOtp(@RequestBody SendOtpRequest request) {
		authFacade.sendOtp(request.destination());
		log.info("Send otp code to destination: {}", request.destination());
	}

	@PostMapping("/verify")
	@ResponseStatus(HttpStatus.OK)
	public AuthResponse verifyOtp(@RequestHeader("X-Device-Id") String deviceId,
			@RequestBody VerifyOtpRequest request) {
		return authFacade.verifyAndAuthenticate(request.destination(), deviceId, request.otp());
	}

	@PostMapping("/refresh")
	@ResponseStatus(HttpStatus.OK)
	public AuthResponse refreshJwtToken(@RequestHeader("X-Device-Id") String deviceId,
			@RequestBody RefreshTokenDto request) {
		return authFacade.refreshToken(request.refreshToken(), deviceId);
	}

	@PostMapping("/google")
	@ResponseStatus(HttpStatus.OK)
	public AuthResponse authenticateWithGoogle(@RequestHeader("X-Device-Id") String deviceId,
			@RequestBody @Valid GoogleAuthRequest request) {
		return authFacade.authenticateWithGoogle(request.idToken(), deviceId);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-Device-Id") String deviceId) {
		authFacade.logout(userId, deviceId);
		log.info("User {} logged out from device {}", userId, deviceId);
	}

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMyAccount(@RequestHeader("X-User-Id") UUID userId) {
		authFacade.deleteAccount(userId);
		log.info("User {} initiated account deletion", userId);
	}
}
