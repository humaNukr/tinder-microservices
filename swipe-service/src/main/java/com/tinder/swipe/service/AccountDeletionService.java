package com.tinder.swipe.service;

import com.tinder.swipe.properties.RedisPrefixesProperties;
import com.tinder.swipe.repository.SwipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final SwipeRepository swipeRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisPrefixesProperties redisPrefixesProperties;

    @Transactional
    public void deleteUserData(UUID userId) {
        int deletedSwipes = swipeRepository.deleteAllForUser(userId);
        stringRedisTemplate.delete(redisPrefixesProperties.limitKeyPrefix() + userId);
        log.info("Deleted {} swipe rows and rate-limit key for user {}", deletedSwipes, userId);
    }
}
