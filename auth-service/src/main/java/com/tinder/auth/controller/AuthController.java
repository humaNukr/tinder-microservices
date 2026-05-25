package com.tinder.auth.controller;

import com.tinder.auth.dto.auth.AuthResponse;
import com.tinder.auth.dto.auth.ExternalAuthRequest;
import com.tinder.auth.dto.jwt.RefreshTokenDto;
import com.tinder.auth.dto.otp.SendOtpRequest;
import com.tinder.auth.dto.otp.VerifyOtpRequest;
import com.tinder.auth.entity.User;
import com.tinder.auth.service.interfaces.AuthFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@Validated
public class AuthController {

	private final AuthFacade authFacade;

	@PostMapping("/send-otp")
	@ResponseStatus(HttpStatus.OK)
	public void sendOtp(@RequestBody @Valid SendOtpRequest request) {
		authFacade.sendOtp(request.destination(), request.channel());
		log.info("Send otp code to destination: {}", request.destination());
	}

	@PostMapping("/verify")
	@ResponseStatus(HttpStatus.OK)
	public AuthResponse verifyOtp(@RequestHeader("X-Device-Id") @NotBlank @Size(max = 128) String deviceId,
			@RequestBody @Valid VerifyOtpRequest request) {
		return authFacade.verifyAndAuthenticate(request.destination(), deviceId, request.otp());
	}

	@PostMapping("/refresh")
	@ResponseStatus(HttpStatus.OK)
	public AuthResponse refreshJwtToken(@RequestHeader("X-Device-Id") @NotBlank @Size(max = 128) String deviceId,
			@RequestBody @Valid RefreshTokenDto request) {
		return authFacade.refreshToken(request.refreshToken(), deviceId);
	}

	@PostMapping("/oauth/{provider}")
	@ResponseStatus(HttpStatus.OK)
	public AuthResponse authenticateWithExternalProvider(@PathVariable String provider,
			@RequestHeader("X-Device-Id") @NotBlank @Size(max = 128) String deviceId,
			@RequestBody @Valid ExternalAuthRequest request) {
		User.AuthProvider authProvider = User.AuthProvider.valueOf(provider.toUpperCase());

		return authFacade.authenticateWithExternalProvider(request.token(), deviceId, authProvider);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestHeader("X-User-Id") @NotNull UUID userId,
			@RequestHeader("X-Device-Id") @NotBlank @Size(max = 128) String deviceId) {
		authFacade.logout(userId, deviceId);
		log.info("User {} logged out from device {}", userId, deviceId);
	}

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMyAccount(@RequestHeader("X-User-Id") @NotNull UUID userId) {
		authFacade.deleteAccount(userId);
		log.info("User {} initiated account deletion", userId);
	}
}
