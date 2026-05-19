package com.tinder.auth.service.interfaces;

import java.util.UUID;

public interface TokenService {
    void storeRefreshToken(UUID userId, String deviceId, String token);

    String getRefreshToken(UUID userId, String deviceId);

    void deleteRefreshToken(UUID userId, String deviceId);

    void deleteAllUserTokens(UUID userId);
}
