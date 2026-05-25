package com.tinder.auth.service.interfaces;

import com.tinder.auth.dto.otp.DeliveryChannel;

public interface OtpService {
	void generateAndSendOtp(String destination, DeliveryChannel channel);

	boolean validateOtp(String identifier, String code);
}
