package com.tinder.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.tinder.auth.exception.ExternalAuthVerificationException;
import com.tinder.auth.properties.GoogleProperties;
import com.tinder.auth.service.impl.GoogleTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierTest {

	@Mock
	private GoogleIdTokenVerifier googleIdTokenVerifier;

	@Mock
	private GoogleProperties googleProperties;

	private JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

	private GoogleTokenVerifier googleTokenVerifier;

	private MockHttpTransport httpTransport;

	@BeforeEach
	void setUp() {
		httpTransport = new MockHttpTransport() {
			@Override
			public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
				return new MockLowLevelHttpRequest() {
					@Override
					public LowLevelHttpResponse execute() throws IOException {
						MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
						response.setStatusCode(200);
						response.setContentType(Json.MEDIA_TYPE);
						response.setContent("{\"id_token\": \"fake-id-token\"}");
						return response;
					}
				};
			}
		};

		when(googleProperties.clientId()).thenReturn("client-id");
		when(googleProperties.clientSecret()).thenReturn("client-secret");

		googleTokenVerifier = new GoogleTokenVerifier(googleIdTokenVerifier, googleProperties, httpTransport,
				jsonFactory);
	}

	@Test
	void verifyTokenAndGetEmail_Success_ReturnsEmail() throws Exception {
		String expectedEmail = "test@google.com";
		GoogleIdToken mockToken = mock(GoogleIdToken.class);
		GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
		payload.setEmail(expectedEmail);
		payload.setEmailVerified(true);

		when(googleIdTokenVerifier.verify(anyString())).thenReturn(mockToken);
		when(mockToken.getPayload()).thenReturn(payload);

		String actualEmail = googleTokenVerifier.verifyTokenAndGetEmail("valid-token");

		assertEquals(expectedEmail, actualEmail);
	}

	@Test
	void verifyTokenAndGetEmail_NullToken_ThrowsException() throws Exception {
		when(googleIdTokenVerifier.verify(anyString())).thenReturn(null);

		ExternalAuthVerificationException exception = assertThrows(ExternalAuthVerificationException.class,
				() -> googleTokenVerifier.verifyTokenAndGetEmail("invalid-token"));

		assertAll(() -> assertEquals(ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, exception.getType()),
				() -> assertEquals("Invalid token", exception.getMessage()));
	}

	@Test
	void verifyTokenAndGetEmail_EmailNotVerified_ThrowsException() throws Exception {
		GoogleIdToken mockToken = mock(GoogleIdToken.class);
		GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
		payload.setEmail("test@google.com");
		payload.setEmailVerified(false);

		when(googleIdTokenVerifier.verify(anyString())).thenReturn(mockToken);
		when(mockToken.getPayload()).thenReturn(payload);

		ExternalAuthVerificationException exception = assertThrows(ExternalAuthVerificationException.class,
				() -> googleTokenVerifier.verifyTokenAndGetEmail("unverified-token"));

		assertAll(() -> assertEquals(ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, exception.getType()),
				() -> assertEquals("Email not verified by Google", exception.getMessage()));
	}

	@Test
	void verifyTokenAndGetEmail_IOException_ThrowsNetworkException() throws Exception {
		when(googleIdTokenVerifier.verify(anyString())).thenThrow(new IOException("Connection reset"));

		ExternalAuthVerificationException exception = assertThrows(ExternalAuthVerificationException.class,
				() -> googleTokenVerifier.verifyTokenAndGetEmail("some-token"));

		assertAll(() -> assertEquals(ExternalAuthVerificationException.ErrorType.NETWORK_ERROR, exception.getType()),
				() -> assertEquals("Network error during verification", exception.getMessage()));
	}

	@Test
	void verifyTokenAndGetEmail_GeneralSecurityException_ThrowsInvalidTokenException() throws Exception {
		when(googleIdTokenVerifier.verify(anyString())).thenThrow(new GeneralSecurityException("Invalid signature"));

		ExternalAuthVerificationException exception = assertThrows(ExternalAuthVerificationException.class,
				() -> googleTokenVerifier.verifyTokenAndGetEmail("some-token"));

		assertAll(() -> assertEquals(ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, exception.getType()),
				() -> assertEquals("Invalid authorization code or signature", exception.getMessage()));
	}
}
