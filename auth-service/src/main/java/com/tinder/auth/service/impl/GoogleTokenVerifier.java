package com.tinder.auth.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.tinder.auth.exception.ExternalAuthVerificationException;
import com.tinder.auth.service.interfaces.ExternalTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenVerifier implements ExternalTokenVerifier {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Override
    public String verifyTokenAndGetEmail(String idToken) {
        try {
            GoogleIdToken idTokenObj = googleIdTokenVerifier.verify(idToken);

            if (idTokenObj == null) {
                log.warn("Google token verification failed: token is null, invalid or expired");
                throw new ExternalAuthVerificationException("Invalid token",
                        ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, null);
            }

            GoogleIdToken.Payload payload = idTokenObj.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                log.warn("Google token verification failed: email {} is not verified by Google", payload.getEmail());
                throw new ExternalAuthVerificationException("Email not verified by Google",
                        ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, null);
            }

            log.debug("Successfully verified Google token for email: {}", payload.getEmail());
            return payload.getEmail();

        } catch (IOException e) {
            log.error("Network error while verifying Google token with Google servers", e);
            throw new ExternalAuthVerificationException("Network error during verification",
                    ExternalAuthVerificationException.ErrorType.NETWORK_ERROR, e);
        } catch (GeneralSecurityException e) {
            log.warn("Security exception (invalid signature) while verifying Google token", e);
            throw new ExternalAuthVerificationException("Invalid token signature",
                    ExternalAuthVerificationException.ErrorType.INVALID_TOKEN, e);
        }
    }
}
