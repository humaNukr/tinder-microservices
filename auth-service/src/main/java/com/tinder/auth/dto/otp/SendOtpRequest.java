package com.tinder.auth.dto.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendOtpRequest(@NotBlank @Size(max = 255) String destination, @NotNull DeliveryChannel channel) {
}
