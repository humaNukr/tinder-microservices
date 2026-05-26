package com.tinder.swipe.exception;

import com.tinder.swipe.controller.SwipeController;
import com.tinder.swipe.dto.swipe.SwipeRequestDto;
import com.tinder.swipe.dto.swipe.SwipeResponseDto;
import com.tinder.swipe.service.interfaces.SwipeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SwipeController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SwipeService swipeService;

    @Nested
    @DisplayName("HTTP error mapping")
    class HttpErrorMapping {

        @Test
        @DisplayName("returns 400 on validation failure")
        void invalidBody_Returns400() throws Exception {
            mockMvc.perform(post("/api/v1/swipes")
                            .header("X-User-Id", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"isLiked\":true}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.details.targetId").exists());
        }

        @Test
        @DisplayName("returns 400 when actor swipes themselves")
        void selfSwipe_Returns400() throws Exception {
            UUID userId = UUID.randomUUID();
            when(swipeService.processSwipe(eq(userId), any(SwipeRequestDto.class)))
                    .thenThrow(new IllegalArgumentException("Actor ID and Target ID cannot be the same"));

            mockMvc.perform(post("/api/v1/swipes")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetId\":\"" + userId + "\",\"isLiked\":true}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Actor ID and Target ID cannot be the same"));
        }

        @Test
        @DisplayName("returns 409 on duplicate swipe")
        void duplicateSwipe_Returns409() throws Exception {
            UUID userId = UUID.randomUUID();
            when(swipeService.processSwipe(eq(userId), any(SwipeRequestDto.class)))
                    .thenThrow(new SwipeAlreadyExistsException("You have already swiped this user."));

            mockMvc.perform(post("/api/v1/swipes")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetId\":\"" + UUID.randomUUID() + "\",\"isLiked\":true}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 429 when rate limit exceeded")
        void rateLimitExceeded_Returns429() throws Exception {
            UUID userId = UUID.randomUUID();
            when(swipeService.processSwipe(eq(userId), any(SwipeRequestDto.class)))
                    .thenThrow(new TooManyRequestsException("You have exceeded your daily like limit."));

            mockMvc.perform(post("/api/v1/swipes")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetId\":\"" + UUID.randomUUID() + "\",\"isLiked\":true}"))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("returns 200 on successful swipe")
        void validSwipe_Returns200() throws Exception {
            UUID userId = UUID.randomUUID();
            UUID targetId = UUID.randomUUID();
            when(swipeService.processSwipe(eq(userId), any(SwipeRequestDto.class)))
                    .thenReturn(new SwipeResponseDto(false));

            mockMvc.perform(post("/api/v1/swipes")
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetId\":\"" + targetId + "\",\"isLiked\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isMatched").value(false));
        }
    }
}
