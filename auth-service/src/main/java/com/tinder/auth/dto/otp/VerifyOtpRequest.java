package com.tinder.auth.dto.otp;

public record VerifyOtpRequest(String destination, String otp) {
}
