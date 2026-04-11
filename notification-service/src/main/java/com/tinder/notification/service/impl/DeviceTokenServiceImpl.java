package com.tinder.notification.service.impl;

import com.tinder.notification.dto.SaveTokenRequest;
import com.tinder.notification.entity.DeviceToken;
import com.tinder.notification.mapper.DeviceTokenMapper;
import com.tinder.notification.repository.DeviceTokenRepository;
import com.tinder.notification.repository.projection.DeviceTokenInfo;
import com.tinder.notification.service.interfaces.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenServiceImpl implements DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenMapper deviceTokenMapper;

    @Override
    @Transactional
    public void saveDeviceToken(UUID userId, SaveTokenRequest request) {
        Optional<DeviceToken> token = deviceTokenRepository.findByToken(request.token());

        if (token.isPresent()) {
            DeviceToken deviceToken = token.get();

            if (!deviceToken.getUserId().equals(userId)) {
                deviceToken.setUserId(userId);
            }

            return;
        }

        DeviceToken deviceToken = deviceTokenMapper.toEntity(request);
        deviceToken.setUserId(userId);

        deviceTokenRepository.save(deviceToken);
    }

    @Transactional
    public void deleteToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        log.info("Removing invalid/unregistered FCM token from database: {}", token);
        deviceTokenRepository.deleteByToken(token);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceTokenInfo> getUserTokens(UUID userId) {
        return deviceTokenRepository.findAllByUserId(userId);
    }
}
