package com.tinder.notification.service.impl;

import com.tinder.notification.dto.SaveTokenRequest;
import com.tinder.notification.entity.DeviceToken;
import com.tinder.notification.mapper.DeviceTokenMapper;
import com.tinder.notification.repository.DeviceTokenRepository;
import com.tinder.notification.repository.projection.DeviceTokenInfo;
import com.tinder.notification.service.interfaces.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

    @Override
    @Transactional(readOnly = true)
    public List<DeviceTokenInfo> getUserTokens(UUID userId) {
        return deviceTokenRepository.findAllByUserId(userId);
    }
}
