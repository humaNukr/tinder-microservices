package com.tinder.auth.service.interfaces;

public interface OtpService {
	void generateAndSendOtp(String identifier);
	boolean validateOtp(String identifier, String code);
}
