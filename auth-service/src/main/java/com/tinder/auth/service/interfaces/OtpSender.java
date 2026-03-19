package com.tinder.auth.service.interfaces;

public interface OtpSender {
    void sendOtp(String destination, Integer otp);
    boolean supports(String identifier);
}
