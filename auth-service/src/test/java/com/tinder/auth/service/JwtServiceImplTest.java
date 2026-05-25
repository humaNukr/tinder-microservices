package com.tinder.auth.service;

import com.tinder.auth.entity.User;
import com.tinder.auth.exception.AuthenticationFailedException;
import com.tinder.auth.properties.JwtProperties;
import com.tinder.auth.service.impl.JwtServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

	private final String secretKeyBase64 = "NmE1NjM1NGY1YjM3MzU2ZTU5MzM1MzQxNDU1OTQzNGU0ZjU1MzQ2ODU5MzQ2YjZhNTU=";
	private final UUID userId = UUID.randomUUID();
	@Mock
	private JwtProperties jwtProperties;
	@InjectMocks
	private JwtServiceImpl jwtService;
	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = new User();
		ReflectionTestUtils.setField(testUser, "id", userId);
		ReflectionTestUtils.setField(testUser, "email", "test@tinder.com");
		ReflectionTestUtils.setField(testUser, "role", User.Role.USER);

		lenient().when(jwtProperties.secret()).thenReturn(secretKeyBase64);
		lenient().when(jwtProperties.accessTokenExpirationMs()).thenReturn(900000L);
		lenient().when(jwtProperties.refreshTokenExpirationMs()).thenReturn(604800000L);

		ReflectionTestUtils.invokeMethod(jwtService, "init");
	}

	@Test
	void generateAccessToken_ValidUser_ReturnsValidToken() {
		String token = jwtService.generateAccessToken(testUser);

		assertNotNull(token);

		String extractedId = jwtService.extractUserId(token);
		assertEquals(userId.toString(), extractedId);
	}

	@Test
	void generateRefreshToken_ValidUser_ReturnsValidToken() {
		String token = jwtService.generateRefreshToken(testUser);

		assertNotNull(token);

		String extractedId = jwtService.extractUserId(token);
		assertEquals(userId.toString(), extractedId);
	}

	@Test
	void extractUserId_ValidToken_ReturnsId() {
		String token = jwtService.generateAccessToken(testUser);
		String extractedId = jwtService.extractUserId(token);

		assertEquals(userId.toString(), extractedId);
	}

	@Test
	void extractUserId_InvalidTokenFormat_ThrowsException() {
		assertThrows(AuthenticationFailedException.class, () -> jwtService.extractUserId("invalid.token.format"));
	}

	@Test
	void extractUserId_ExpiredToken_ThrowsException() {
		String expiredToken = Jwts.builder().subject(userId.toString())
				.issuedAt(new Date(System.currentTimeMillis() - 10000))
				.expiration(new Date(System.currentTimeMillis() - 1000))
				.signWith(Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(secretKeyBase64))).compact();

		AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
				() -> jwtService.extractUserId(expiredToken));

		assertEquals("Invalid or expired JWT token", exception.getMessage());
	}

	@Test
	void extractUserId_WrongSignature_ThrowsException() {
		String wrongKeyBase64 = "YWE1NjM1NGY1YjM3MzU2ZTU5MzM1MzQxNDU1OTQzNGU0ZjU1MzQ2ODU5MzQ2YjZhNTU=";
		String forgedToken = Jwts.builder().subject(userId.toString()).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 10000))
				.signWith(Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(wrongKeyBase64))).compact();

		assertThrows(AuthenticationFailedException.class, () -> jwtService.extractUserId(forgedToken));
	}
}
