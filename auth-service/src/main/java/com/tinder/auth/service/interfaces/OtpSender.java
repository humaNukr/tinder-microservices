package com.tinder.auth.service.interfaces;

import com.tinder.auth.dto.otp.DeliveryChannel;

public interface OtpSender {
    void sendOtp(String destination, Integer otp);

    boolean supports(DeliveryChannel channel);
}
