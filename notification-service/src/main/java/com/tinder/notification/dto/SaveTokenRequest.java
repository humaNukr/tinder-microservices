package com.tinder.notification.dto;

import com.tinder.notification.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveTokenRequest(
        @NotBlank
        String token,
        @NotNull
        DeviceType deviceType
) {
}
