package com.tinder.notification.repository.projection;

import java.util.UUID;

public interface DeviceTokenInfo {
    UUID getId();

    UUID getUserId();

    String getDeviceType();

    String getToken();
}
