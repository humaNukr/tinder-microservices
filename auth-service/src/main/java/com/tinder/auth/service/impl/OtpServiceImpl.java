package com.tinder.auth.service.impl;

import com.tinder.auth.dto.otp.DeliveryChannel;
import com.tinder.auth.service.interfaces.OtpSender;
import com.tinder.auth.service.interfaces.OtpService;
import com.tinder.auth.service.interfaces.OtpStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

	private final List<OtpSender> otpSenders;
	private final OtpStorage otpStorage;
	private final SecureRandom secureRandom;

	@Override
	public void generateAndSendOtp(String destination, DeliveryChannel channel) {
		otpStorage.checkAndIncrementRateLimit(destination);

		String code = String.valueOf(100000 + secureRandom.nextInt(900000));

		otpStorage.saveOtp(destination, code);
		OtpSender sender = otpSenders.stream().filter(s -> s.supports(channel)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No sender found for channel: " + channel));
		sender.sendOtp(destination, Integer.parseInt(code));

		log.info("OTP sent via {} to {}", channel, destination);
	}

	@Override
	public boolean validateOtp(String identifier, String code) {
		otpStorage.checkAndIncrementVerificationAttempts(identifier);

		String savedCode = otpStorage.getOtp(identifier);

		if (savedCode == null || code == null) {
			return false;
		}

		if (MessageDigest.isEqual(savedCode.getBytes(), code.getBytes())) {
			otpStorage.deleteOtp(identifier);
			return true;
		}

		return false;
	}
}
