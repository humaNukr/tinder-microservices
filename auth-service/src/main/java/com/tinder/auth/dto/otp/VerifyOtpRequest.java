package com.tinder.auth.dto.otp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(@NotBlank String destination,

		@NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits") String otp) {
}
