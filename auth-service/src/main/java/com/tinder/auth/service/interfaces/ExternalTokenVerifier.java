package com.tinder.auth.service.interfaces;

public interface ExternalTokenVerifier {
	String verifyTokenAndGetEmail(String idToken);
}
