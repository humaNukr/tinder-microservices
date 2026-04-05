package com.tinder.notification.service.interfaces;

import com.tinder.notification.dto.SaveTokenRequest;

import java.util.UUID;

public interface DeviceTokenService {
    void saveDeviceToken (UUID userId, SaveTokenRequest request);
}
