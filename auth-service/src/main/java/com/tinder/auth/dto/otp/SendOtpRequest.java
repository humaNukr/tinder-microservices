package com.tinder.auth.dto.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(@NotBlank String destination, @NotNull DeliveryChannel channel) {
}
