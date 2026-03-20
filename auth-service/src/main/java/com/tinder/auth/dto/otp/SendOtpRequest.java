package com.tinder.auth.dto.otp;

import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(@NotBlank String destination) {
}
