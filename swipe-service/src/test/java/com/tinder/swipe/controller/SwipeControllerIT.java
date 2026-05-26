package com.tinder.swipe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.swipe.repository.OutboxRepository;
import com.tinder.swipe.repository.SwipeRepository;
import com.tinder.swipe.util.BaseIT;
import com.tinder.swipe.util.SwipeTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SwipeController — Integration Tests")
class SwipeControllerIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        swipeRepository.deleteAll();
        outboxRepository.deleteAll();
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("POST /api/v1/swipes — returns 200 and match=false on first like")
    void processSwipe_FirstLike_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/swipes")
                        .header("X-User-Id", SwipeTestFixtures.USER_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMatched").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/swipes — returns 409 on duplicate swipe")
    void processSwipe_Duplicate_Returns409() throws Exception {
        String body = objectMapper.writeValueAsString(
                SwipeTestFixtures.like(SwipeTestFixtures.USER_TWO));

        mockMvc.perform(post("/api/v1/swipes")
                        .header("X-User-Id", SwipeTestFixtures.USER_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/swipes")
                        .header("X-User-Id", SwipeTestFixtures.USER_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/v1/swipes — returns 400 when targetId is missing")
    void processSwipe_MissingTargetId_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/swipes")
                        .header("X-User-Id", SwipeTestFixtures.USER_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isLiked\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.targetId").exists());
    }
}
