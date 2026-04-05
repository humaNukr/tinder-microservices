package com.tinder.notification.repository;

import com.tinder.notification.entity.DeviceToken;
import com.tinder.notification.repository.projection.DeviceTokenInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    
    List<DeviceTokenInfo> findAllByUserId(UUID userId);
    
    Optional<DeviceToken> findByToken(String token);
    
    void deleteByToken(String token);
}