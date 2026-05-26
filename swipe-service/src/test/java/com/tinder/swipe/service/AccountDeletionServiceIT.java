package com.tinder.swipe.service;

import com.tinder.swipe.entity.Swipe;
import com.tinder.swipe.properties.RedisPrefixesProperties;
import com.tinder.swipe.repository.SwipeRepository;
import com.tinder.swipe.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("AccountDeletionService — Integration Tests")
class AccountDeletionServiceIT extends BaseIT {

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisPrefixesProperties redisPrefixesProperties;

    @BeforeEach
    void setUp() {
        swipeRepository.deleteAll();
    }

    @Test
    @DisplayName("deleteUserData removes swipes and rate-limit key")
    void deleteUserData_RemovesSwipesAndRedisKey() {
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID user1 = userId.compareTo(otherUser) < 0 ? userId : otherUser;
        UUID user2 = userId.compareTo(otherUser) < 0 ? otherUser : userId;

        Swipe swipe = new Swipe();
        swipe.setUser1Id(user1);
        swipe.setUser2Id(user2);
        swipe.setIsLikedByUser1(true);
        swipe.setIsLikedByUser2(false);
        swipeRepository.save(swipe);

        String rateLimitKey = redisPrefixesProperties.limitKeyPrefix() + userId;
        stringRedisTemplate.opsForValue().set(rateLimitKey, "3");

        accountDeletionService.deleteUserData(userId);

        assertEquals(0, swipeRepository.count());
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(rateLimitKey)));
    }
}
