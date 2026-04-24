package com.tinder.auth.service.impl;

import com.tinder.auth.exception.TooManyRequestsException;
import com.tinder.auth.service.interfaces.OtpSender;
import com.tinder.auth.service.interfaces.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
	private final List<OtpSender> otpSenders;
	private final StringRedisTemplate stringRedisTemplate;
	private final SecureRandom secureRandom;

	private static final String OTP_KEY_PREFIX = "otp:";
	private static final Duration OTP_TTL = Duration.ofMinutes(5);
	private static final Integer OTP_RATE_LIMIT_PER_MINUTE = 1;

	@Override
	public void generateAndSendOtp(String identifier) {
		String redisKey = OTP_KEY_PREFIX + identifier;

		Long count = stringRedisTemplate.opsForValue().increment("rate_limit:otp:" + identifier);
		if (count != null && count == 1) {
			stringRedisTemplate.expire("rate_limit:otp:" + identifier, Duration.ofMinutes(1));
		}
		if (count != null && count > OTP_RATE_LIMIT_PER_MINUTE) {
			throw new TooManyRequestsException("Too many requests for otp:" + identifier);
		}

		Integer code = 100000 + secureRandom.nextInt(900000);
		stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(code), OTP_TTL);

		OtpSender sender = otpSenders.stream().filter(s -> s.supports(identifier)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown identifier " + identifier));

		sender.sendOtp(identifier, code);
	}

	@Override
	public boolean validateOtp(String identifier, String code) {
		String redisKey = OTP_KEY_PREFIX + identifier;
		String savedCode = stringRedisTemplate.opsForValue().get(redisKey);

		if (savedCode != null && savedCode.equals(code)) {
			stringRedisTemplate.delete(redisKey);
			return true;
		}
		return false;
	}
}
