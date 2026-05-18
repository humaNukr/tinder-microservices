package com.tinder.auth.service;

import com.tinder.auth.exception.TooManyRequestsException;
import com.tinder.auth.properties.RedisAuthProperties;
import com.tinder.auth.service.impl.RedisOtpStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisOtpStorageTest {

	private final String identifier = "test@test.com";
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;
	@Mock
	private RedisAuthProperties props;
	@InjectMocks
	private RedisOtpStorage redisOtpStorage;

	@BeforeEach
	void setUp() {
		lenient().when(props.otpPrefix()).thenReturn("otp:");
	}

	@Nested
	@DisplayName("OTP CRUD Operations")
	class CrudOperations {

		@Test
        void saveOtp_ValidData_SavesWithTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            Duration ttl = Duration.ofMinutes(5);
            when(props.otpTtl()).thenReturn(ttl);

            redisOtpStorage.saveOtp(identifier, "123456");

            verify(valueOperations).set("otp:" + identifier, "123456", ttl);
        }

		@Test
        void getOtp_ExistingKey_ReturnsCode() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("otp:" + identifier)).thenReturn("654321");

            String result = redisOtpStorage.getOtp(identifier);

            assertEquals("654321", result);
        }

		@Test
		void deleteOtp_ValidIdentifier_DeletesKey() {
			redisOtpStorage.deleteOtp(identifier);

			verify(redisTemplate).delete("otp:" + identifier);
		}
	}

	@Nested
	@DisplayName("Rate Limiting Tests")
	class RateLimitingTests {

		@BeforeEach
        void setUpRateLimiting() {
            when(props.otpRateLimitPrefix()).thenReturn("rl:");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

		@Test
		void checkAndIncrementRateLimit_FirstRequest_IncrementsAndSetsExpire() {
			String key = "rl:" + identifier;
			Duration window = Duration.ofMinutes(1);
			when(props.otpRateLimitWindow()).thenReturn(window);
			when(props.otpRateLimitMaxRequests()).thenReturn(3);
			when(valueOperations.increment(key)).thenReturn(1L);

			redisOtpStorage.checkAndIncrementRateLimit(identifier);

			assertAll(() -> verify(valueOperations).increment(key), () -> verify(redisTemplate).expire(key, window));
		}

		@Test
		void checkAndIncrementRateLimit_SecondRequest_IncrementsWithoutExpire() {
			String key = "rl:" + identifier;
			when(props.otpRateLimitMaxRequests()).thenReturn(3);
			when(valueOperations.increment(key)).thenReturn(2L);

			redisOtpStorage.checkAndIncrementRateLimit(identifier);

			assertAll(() -> verify(valueOperations).increment(key),
					() -> verify(redisTemplate, never()).expire(anyString(), any(Duration.class)));
		}

		@Test
		void checkAndIncrementRateLimit_LimitExceeded_ThrowsException() {
			String key = "rl:" + identifier;
			when(props.otpRateLimitMaxRequests()).thenReturn(3);
			when(valueOperations.increment(key)).thenReturn(4L);

			assertThrows(TooManyRequestsException.class, () -> redisOtpStorage.checkAndIncrementRateLimit(identifier));

			verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
		}
	}
}
