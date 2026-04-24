package com.tinder.auth.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.tinder.auth.exception.ExternalAuthVerificationException;
import com.tinder.auth.service.interfaces.ExternalTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
public class GoogleTokenVerifier implements ExternalTokenVerifier {
	private final GoogleIdTokenVerifier googleIdTokenVerifier;

	@Override
	public String verifyTokenAndGetEmail(String idToken) {
		try {
			GoogleIdToken idTokenObj = googleIdTokenVerifier.verify(idToken);
			if (idTokenObj == null) {
				throw new ExternalAuthVerificationException("Invalid token",
						ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, null);
			}
			GoogleIdToken.Payload payload = idTokenObj.getPayload();
			if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
				throw new ExternalAuthVerificationException("Email not verified by Google",
						ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, null);
			}
			return payload.getEmail();
		} catch (IOException e) {
			throw new ExternalAuthVerificationException("Network error during verification",
					ExternalAuthVerificationException.ErrorType.NETWORK_ERROR, e);
		} catch (GeneralSecurityException e) {
			throw new ExternalAuthVerificationException("Invalid token signature",
					ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, e);
		}
	}
}
