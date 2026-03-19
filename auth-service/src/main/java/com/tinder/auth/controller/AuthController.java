package com.tinder.auth.controller;

import com.tinder.auth.dto.otp.SendOtpRequest;
import com.tinder.auth.dto.otp.VerifyOtpRequest;
import com.tinder.auth.service.interfaces.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {
	private final OtpService otpService;

	@PostMapping("/send-otp")
	public ResponseEntity<Void> sendOtp(@RequestBody SendOtpRequest sendOtpRequest) {
		otpService.generateAndSendOtp(sendOtpRequest.destination());
		log.info("Send otp code to destination: {}", sendOtpRequest.destination());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/verify")
	public ResponseEntity<String> verifyOtp(@RequestBody VerifyOtpRequest verifyOtpRequest) {
		return otpService.validateOtp(verifyOtpRequest.destination(), verifyOtpRequest.otp())
				? ResponseEntity.ok("Success")
				: ResponseEntity.status(401).body("Code invalid");
	}
}
