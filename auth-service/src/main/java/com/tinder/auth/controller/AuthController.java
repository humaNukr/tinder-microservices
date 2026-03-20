package com.tinder.auth.controller;

import com.tinder.auth.dto.AuthResponse;
import com.tinder.auth.dto.jwt.RefreshTokenDto;
import com.tinder.auth.dto.otp.SendOtpRequest;
import com.tinder.auth.dto.otp.VerifyOtpRequest;
import com.tinder.auth.service.interfaces.AuthFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
	public AuthResponse verifyOtp(@RequestBody VerifyOtpRequest request) {
		return authFacade.verifyAndAuthenticate(request.destination(), request.otp());
	}

	@PostMapping("/refresh")
	@ResponseStatus(HttpStatus.OK)
	public AuthResponse refreshJwtToken(@RequestBody RefreshTokenDto request) {
		return authFacade.refreshToken(request.refreshToken());
	}
}
