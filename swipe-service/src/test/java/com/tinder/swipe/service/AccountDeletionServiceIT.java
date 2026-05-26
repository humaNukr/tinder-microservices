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
        UUID userIdToDelete = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID otherUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        saveSwipe(userIdToDelete, otherUserId, true, false);

        String rateLimitKey = redisPrefixesProperties.limitKeyPrefix() + userIdToDelete;
        stringRedisTemplate.opsForValue().set(rateLimitKey, "3");

        accountDeletionService.deleteUserData(userIdToDelete);

        assertEquals(0, swipeRepository.count(), "Swipe should be deleted from DB");
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(rateLimitKey)), "Rate limit key should be deleted from Redis");
    }

    private void saveSwipe(UUID initiator, UUID target, boolean initiatorLikes, boolean targetLikes) {
        boolean isInitiatorUser1 = initiator.toString().compareTo(target.toString()) < 0;

        Swipe swipe = new Swipe();
        swipe.setUser1Id(isInitiatorUser1 ? initiator : target);
        swipe.setUser2Id(isInitiatorUser1 ? target : initiator);
        swipe.setIsLikedByUser1(isInitiatorUser1 ? initiatorLikes : targetLikes);
        swipe.setIsLikedByUser2(isInitiatorUser1 ? targetLikes : initiatorLikes);

        swipeRepository.save(swipe);
    }
}
