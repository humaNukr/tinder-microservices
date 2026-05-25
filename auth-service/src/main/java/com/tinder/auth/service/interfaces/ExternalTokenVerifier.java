package com.tinder.auth.service.interfaces;

import com.tinder.auth.entity.User;

public interface ExternalTokenVerifier {
    User.AuthProvider getSupportedProvider();

    String verifyTokenAndGetIdentifier(String token);
}