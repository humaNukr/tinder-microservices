package com.tinder.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.auth.dto.jwt.RefreshTokenDto;
import com.tinder.auth.dto.otp.DeliveryChannel;
import com.tinder.auth.dto.otp.SendOtpRequest;
import com.tinder.auth.dto.otp.VerifyOtpRequest;
import com.tinder.auth.entity.User;
import com.tinder.auth.repository.OutboxRepository;
import com.tinder.auth.repository.UserRepository;
import com.tinder.auth.service.interfaces.JwtService;
import com.tinder.auth.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends BaseIT {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private OutboxRepository outboxRepository;
	@Autowired
	private StringRedisTemplate redisTemplate;
	@Autowired
	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		redisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().flushAll();
	}

	private void setupOtpInRedis(String email, String code) {
		redisTemplate.opsForValue().set("otp:" + email, code, Duration.ofMinutes(5));
	}

	private void setupSessionInRedis(UUID userId, String deviceId, String token) {
		String key = "user:" + userId + ":sessions";
		redisTemplate.opsForHash().put(key, deviceId, token);
	}

	@Nested
	@DisplayName("sendOtp() Validation & Logic")
	class SendOtpTests {

		@Test
		void sendOtp_ValidRequest_Returns200() throws Exception {
			SendOtpRequest request = new SendOtpRequest("test@example.com", DeliveryChannel.EMAIL);

			mockMvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());
		}

		@Test
		void sendOtp_BlankDestination_Returns400() throws Exception {
			SendOtpRequest request = new SendOtpRequest("", DeliveryChannel.EMAIL);

			mockMvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
		}

		@Test
		void sendOtp_RateLimited_Returns429() throws Exception {
			String email = "fast@example.com";
			SendOtpRequest request = new SendOtpRequest(email, DeliveryChannel.EMAIL);

			mockMvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)));

			mockMvc.perform(post("/api/v1/auth/send-otp").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))).andExpect(status().isTooManyRequests());
		}
	}

	@Nested
	@DisplayName("verifyOtp() Validation & Logic")
	class VerifyOtpTests {

		@Test
		void verifyOtp_ValidNewUser_ReturnsTokensAndIsNewTrue() throws Exception {
			String email = "new@test.com";
			String code = "123456";
			setupOtpInRedis(email, code);

			VerifyOtpRequest request = new VerifyOtpRequest(email, code);

			mockMvc.perform(post("/api/v1/auth/verify").header("X-Device-Id", "d1")
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists())
					.andExpect(jsonPath("$.isNewUser").value(true));
		}

		@Test
		void verifyOtp_InvalidOtpFormat_Returns400() throws Exception {
			VerifyOtpRequest request = new VerifyOtpRequest("test@test.com", "123");

			mockMvc.perform(post("/api/v1/auth/verify").header("X-Device-Id", "d1")
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest());
		}

		@Test
		void verifyOtp_WrongCode_Returns401() throws Exception {
			String email = "test@test.com";
			setupOtpInRedis(email, "111111");

			VerifyOtpRequest request = new VerifyOtpRequest(email, "222222");

			mockMvc.perform(post("/api/v1/auth/verify").header("X-Device-Id", "d1")
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("refreshToken() Logic")
	class RefreshTokenTests {

		@Test
		void refreshToken_ValidRequest_ReturnsNewTokens() throws Exception {
			User user = userRepository.save(User.createNewVerifiedUser("refresh@test.com"));
			String deviceId = "laptop";
			String refreshToken = jwtService.generateRefreshToken(user);
			setupSessionInRedis(user.getId(), deviceId, refreshToken);

			RefreshTokenDto request = new RefreshTokenDto(refreshToken);

			mockMvc.perform(post("/api/v1/auth/refresh").header("X-Device-Id", deviceId)
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists());
		}

		@Test
		void refreshToken_InvalidOrStolenToken_Returns401() throws Exception {
			User user = userRepository.save(User.createNewVerifiedUser("stolen@test.com"));
			String validToken = jwtService.generateRefreshToken(user);
			setupSessionInRedis(user.getId(), "device-A", validToken);

			RefreshTokenDto request = new RefreshTokenDto(validToken);

			mockMvc.perform(post("/api/v1/auth/refresh").header("X-Device-Id", "device-B")
					.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("Session & Account Management")
	class ManagementTests {

		@Test
		void logout_ExistingSession_DeletesFromRedis() throws Exception {
			UUID userId = UUID.randomUUID();
			String deviceId = "phone";
			setupSessionInRedis(userId, deviceId, "token");

			mockMvc.perform(post("/api/v1/auth/logout").header("X-User-Id", userId).header("X-Device-Id", deviceId))
					.andExpect(status().isNoContent());

			assertFalse(redisTemplate.opsForHash().hasKey("user:" + userId + ":sessions", deviceId));
		}

		@Test
		void deleteMyAccount_ValidUser_RemovesAllDataAndSchedulesEvent() throws Exception {
			User user = userRepository.save(User.createNewVerifiedUser("rip@test.com"));
			UUID userId = user.getId();
			setupSessionInRedis(userId, "d1", "t1");

			mockMvc.perform(delete("/api/v1/auth/me").header("X-User-Id", userId)).andExpect(status().isNoContent());

			assertAll(() -> assertTrue(userRepository.findById(userId).isEmpty()),
					() -> assertFalse(redisTemplate.hasKey("user:" + userId + ":sessions")), () -> {
						var events = outboxRepository.findAll();
						assertTrue(events.stream().anyMatch(e -> e.getPayload().contains("DELETE_ACCOUNT")));
					});
		}
	}
}
