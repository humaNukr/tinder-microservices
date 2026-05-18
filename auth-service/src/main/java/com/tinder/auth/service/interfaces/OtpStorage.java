package com.tinder.auth.service.interfaces;

public interface OtpStorage {
	void saveOtp(String identifier, String code);

	String getOtp(String identifier);

	void deleteOtp(String identifier);

	void checkAndIncrementRateLimit(String identifier);
}
