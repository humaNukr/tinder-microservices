package com.tinder.notification.service;

import com.tinder.notification.repository.DeviceTokenRepository;
import com.tinder.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void deleteUserData(UUID userId) {
        int notificationsDeleted = notificationRepository.deleteAllByUserId(userId);
        int tokensDeleted = deviceTokenRepository.deleteAllByUserId(userId);
        log.info(
                "Deleted {} notifications and {} device tokens for user {}",
                notificationsDeleted,
                tokensDeleted,
                userId);
    }
}
