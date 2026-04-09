package com.tinder.notification.controller;

import com.tinder.notification.dto.SaveTokenRequest;
import com.tinder.notification.service.interfaces.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/tokens")
@RequiredArgsConstructor
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void saveToken(
            @RequestHeader("X-User-Id") UUID id,
            @RequestBody @Valid SaveTokenRequest request
    ) {
        deviceTokenService.saveDeviceToken(id, request);
    }
}
