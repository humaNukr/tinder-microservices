package com.tinder.notification.service.interfaces;

import com.tinder.notification.dto.SaveTokenRequest;
import com.tinder.notification.repository.projection.DeviceTokenInfo;

import java.util.List;
import java.util.UUID;

public interface DeviceTokenService {
    void saveDeviceToken(UUID userId, SaveTokenRequest request);

    List<DeviceTokenInfo> getUserTokens(UUID userId);

    void deleteToken(String token);
}
