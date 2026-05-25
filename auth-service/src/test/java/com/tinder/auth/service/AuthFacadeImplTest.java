package com.tinder.auth.service;

import com.tinder.auth.dto.auth.AuthResponse;
import com.tinder.auth.dto.otp.DeliveryChannel;
import com.tinder.auth.dto.user.UserResult;
import com.tinder.auth.entity.User;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.exception.AuthenticationFailedException;
import com.tinder.auth.publisher.UserActivityPublisher;
import com.tinder.auth.service.impl.AuthFacadeImpl;
import com.tinder.auth.service.interfaces.ExternalTokenVerifier;
import com.tinder.auth.service.interfaces.JwtService;
import com.tinder.auth.service.interfaces.OtpService;
import com.tinder.auth.service.interfaces.TokenService;
import com.tinder.auth.service.interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthFacadeImplTest {

	private final String identifier = "test@example.com";
	private final String deviceId = "device-123";
	private final String accessToken = "access.token.here";
	private final String refreshToken = "refresh.token.here";
	private final UUID userId = UUID.randomUUID();

	@Mock
	private OtpService otpService;
	@Mock
	private TokenService tokenService;
	@Mock
	private JwtService jwtService;
	@Mock
	private UserService userService;
	@Mock
	private UserActivityPublisher activityPublisher;
	@Mock
	private ExternalTokenVerifier googleVerifier;

	@InjectMocks
	private AuthFacadeImpl authFacade;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = new User();
		ReflectionTestUtils.setField(testUser, "id", userId);
		ReflectionTestUtils.setField(testUser, "email", identifier);

		ReflectionTestUtils.setField(authFacade, "externalVerifiers", List.of(googleVerifier));
	}

	@Nested
	@DisplayName("sendOtp() Tests")
	class SendOtpTests {
		@Test
		void sendOtp_ValidParameters_CallsOtpService() {
			authFacade.sendOtp(identifier, DeliveryChannel.EMAIL);
			verify(otpService).generateAndSendOtp(identifier, DeliveryChannel.EMAIL);
		}
	}

	@Nested
	@DisplayName("verifyAndAuthenticate() Tests")
	class VerifyAndAuthenticateTests {
		@Test
		void verifyAndAuthenticate_ValidOtp_ReturnsAuthResponse() {
			String code = "123456";
			when(otpService.validateOtp(identifier, code)).thenReturn(true);
			when(userService.findOrCreateUser(identifier, User.AuthProvider.EMAIL_OTP))
					.thenReturn(new UserResult(testUser, true));
			when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
			when(jwtService.generateRefreshToken(testUser)).thenReturn(refreshToken);

			AuthResponse response = authFacade.verifyAndAuthenticate(identifier, deviceId, code);

			assertAll(() -> assertNotNull(response), () -> assertEquals(accessToken, response.accessToken()),
					() -> assertEquals(refreshToken, response.refreshToken()), () -> assertTrue(response.isNewUser()));

			verify(tokenService).storeRefreshToken(userId, deviceId, refreshToken);
			verify(activityPublisher).publishActivity(userId, ActivityType.LOGIN);
		}

		@Test
		void verifyAndAuthenticate_InvalidOtp_ThrowsException() {
			String code = "wrong";
			when(otpService.validateOtp(identifier, code)).thenReturn(false);

			assertThrows(AuthenticationFailedException.class,
					() -> authFacade.verifyAndAuthenticate(identifier, deviceId, code));

			verifyNoInteractions(userService, jwtService, tokenService, activityPublisher);
		}
	}

	@Nested
	@DisplayName("refreshToken() Tests")
	class RefreshTokenTests {
		@Test
        void refreshToken_ValidToken_ReturnsNewTokens() {
            when(jwtService.extractUserId(refreshToken)).thenReturn(userId.toString());
            when(tokenService.getRefreshToken(userId, deviceId)).thenReturn(refreshToken);
            when(userService.findUserById(userId)).thenReturn(testUser);

            String newAccessToken = "new.access.token";
            String newRefreshToken = "new.refresh.token";
            when(jwtService.generateAccessToken(testUser)).thenReturn(newAccessToken);
            when(jwtService.generateRefreshToken(testUser)).thenReturn(newRefreshToken);

            AuthResponse response = authFacade.refreshToken(refreshToken, deviceId);

            assertAll(
                    () -> assertNotNull(response),
                    () -> assertEquals(newAccessToken, response.accessToken()),
                    () -> assertEquals(newRefreshToken, response.refreshToken()),
                    () -> assertFalse(response.isNewUser())
            );

            verify(tokenService).storeRefreshToken(userId, deviceId, newRefreshToken);
            verify(activityPublisher).publishActivity(userId, ActivityType.TOKEN_REFRESH);
        }

		@Test
        void refreshToken_TokenMissingInStorage_ThrowsException() {
            when(jwtService.extractUserId(refreshToken)).thenReturn(userId.toString());
            when(tokenService.getRefreshToken(userId, deviceId)).thenReturn(null);

            assertThrows(AuthenticationFailedException.class,
                    () -> authFacade.refreshToken(refreshToken, deviceId));

            verifyNoInteractions(userService, activityPublisher);
            verify(jwtService, never()).generateAccessToken(any());
        }

		@Test
        void refreshToken_TokenMismatch_ThrowsException() {
            when(jwtService.extractUserId(refreshToken)).thenReturn(userId.toString());
            when(tokenService.getRefreshToken(userId, deviceId)).thenReturn("different.token");

            assertThrows(AuthenticationFailedException.class,
                    () -> authFacade.refreshToken(refreshToken, deviceId));

            verifyNoInteractions(userService, activityPublisher);
        }
	}

	@Nested
	@DisplayName("authenticateWithExternalProvider() Tests")
	class AuthenticateWithExternalProviderTests {

		@Test
		void authenticateWithExternalProvider_ValidGoogleToken_ReturnsAuthResponse() {
			String idToken = "google.id.token";

			when(googleVerifier.getSupportedProvider()).thenReturn(User.AuthProvider.GOOGLE);
			when(googleVerifier.verifyTokenAndGetIdentifier(idToken)).thenReturn(identifier);

			when(userService.findOrCreateUser(identifier, User.AuthProvider.GOOGLE))
					.thenReturn(new UserResult(testUser, false));
			when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
			when(jwtService.generateRefreshToken(testUser)).thenReturn(refreshToken);

			AuthResponse response = authFacade.authenticateWithExternalProvider(idToken, deviceId,
					User.AuthProvider.GOOGLE);

			assertAll(() -> assertNotNull(response), () -> assertEquals(accessToken, response.accessToken()),
					() -> assertEquals(refreshToken, response.refreshToken()), () -> assertFalse(response.isNewUser()));

			verify(tokenService).storeRefreshToken(userId, deviceId, refreshToken);
			verify(activityPublisher).publishActivity(userId, ActivityType.LOGIN);
		}

		@Test
		void authenticateWithExternalProvider_UnsupportedProvider_ThrowsException() {
			String idToken = "apple.id.token";
			when(googleVerifier.getSupportedProvider()).thenReturn(User.AuthProvider.GOOGLE);

			assertThrows(IllegalArgumentException.class,
					() -> authFacade.authenticateWithExternalProvider(idToken, deviceId, User.AuthProvider.PHONE));

			verifyNoInteractions(userService, jwtService, tokenService);
		}
	}

	@Nested
	@DisplayName("Account Management Tests")
	class AccountManagementTests {
		@Test
		void logout_ValidRequest_DeletesRefreshToken() {
			authFacade.logout(userId, deviceId);
			verify(tokenService).deleteRefreshToken(userId, deviceId);
		}

		@Test
		void deleteAccount_ValidRequest_DeletesUserAndTokens() {
			authFacade.deleteAccount(userId);
			verify(tokenService).deleteAllUserTokens(userId);
			verify(userService).deleteUser(userId);
		}
	}
}
