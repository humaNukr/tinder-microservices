package com.tinder.auth.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.tinder.auth.entity.User;
import com.tinder.auth.exception.ExternalAuthVerificationException;
import com.tinder.auth.properties.GoogleProperties;
import com.tinder.auth.service.interfaces.ExternalTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenVerifier implements ExternalTokenVerifier {

	private final GoogleIdTokenVerifier googleIdTokenVerifier;
	private final GoogleProperties googleProperties;
	private final HttpTransport httpTransport;
	private final JsonFactory jsonFactory;

	public String verifyTokenAndGetEmail(String authorizationCode) {
		try {
			log.debug("Exchanging authorization code for Google ID token");
			GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(httpTransport, jsonFactory,
					"https://oauth2.googleapis.com/token", googleProperties.clientId(), googleProperties.clientSecret(),
					authorizationCode, "postmessage").execute();

			String idTokenString = tokenResponse.getIdToken();
			if (idTokenString == null) {
				log.warn("Google token exchange failed: no ID token in response");
				throw new ExternalAuthVerificationException("No ID token returned from Google",
						ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, null);
			}

			GoogleIdToken idTokenObj = googleIdTokenVerifier.verify(idTokenString);

			if (idTokenObj != null) {
				GoogleIdToken.Payload payload = idTokenObj.getPayload();

				if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
					log.warn("Google token verification failed: email {} is not verified by Google",
							payload.getEmail());
					throw new ExternalAuthVerificationException("Email not verified by Google",
							ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, null);
				}

				log.debug("Successfully verified Google ID token for email: {}", payload.getEmail());
				return payload.getEmail();
			} else {
				log.warn("Google token verification failed: ID token is null, invalid or expired");
				throw new ExternalAuthVerificationException("Invalid token",
						ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, null);
			}
		} catch (IOException e) {
			log.error("Network error while exchanging Google code with Google servers", e);
			throw new ExternalAuthVerificationException("Network error during verification",
					ExternalAuthVerificationException.ErrorType.NETWORK_ERROR, e);
		} catch (java.security.GeneralSecurityException e) {
			log.error("Failed to verify Google ID token signature", e);
			throw new ExternalAuthVerificationException("Invalid authorization code or signature",
					ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, e);
		}
	}

	@Override
	public User.AuthProvider getSupportedProvider() {
		return User.AuthProvider.GOOGLE;
	}

	@Override
	public String verifyTokenAndGetIdentifier(String token) {
		return verifyTokenAndGetEmail(token);
	}
}
