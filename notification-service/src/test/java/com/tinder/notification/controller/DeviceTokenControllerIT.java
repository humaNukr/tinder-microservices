package com.tinder.notification.controller;

import com.tinder.notification.dto.SaveTokenRequest;
import com.tinder.notification.enums.DeviceType;
import com.tinder.notification.provider.PushSender;
import com.tinder.notification.repository.DeviceTokenRepository;
import com.tinder.notification.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Device Token Controller — Integration Tests")
class DeviceTokenControllerIT extends BaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deviceTokenRepository.deleteAll();
    }

    private HttpEntity<SaveTokenRequest> request(UUID owner, SaveTokenRequest body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", owner.toString());
        return new HttpEntity<>(body, headers);
    }

    private SaveTokenRequest tokenRequest(String token) {
        return new SaveTokenRequest(token, DeviceType.ANDROID);
    }

    @Nested
    @DisplayName("POST /api/v1/notifications/tokens")
    class SaveToken {

        @Test
        @DisplayName("returns 201 and persists device token")
        void newToken_ReturnsCreated() {
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    "/api/v1/notifications/tokens",
                    request(userId, tokenRequest("fcm-token-new")),
                    Void.class
            );

            assertAll(
                    () -> assertEquals(HttpStatus.CREATED, response.getStatusCode()),
                    () -> assertTrue(deviceTokenRepository.findByToken("fcm-token-new").isPresent()),
                    () -> assertEquals(userId, deviceTokenRepository.findByToken("fcm-token-new").get().getUserId())
            );
        }

        @Test
        @DisplayName("reassigns existing token to new user without duplicate row")
        void tokenExistsForOtherUser_ReassignsUserId() {
            UUID previousOwner = UUID.randomUUID();
            restTemplate.postForEntity(
                    "/api/v1/notifications/tokens",
                    request(previousOwner, tokenRequest("shared-token")),
                    Void.class
            );

            restTemplate.postForEntity(
                    "/api/v1/notifications/tokens",
                    request(userId, tokenRequest("shared-token")),
                    Void.class
            );

            assertAll(
                    () -> assertEquals(1, deviceTokenRepository.count()),
                    () -> assertEquals(userId, deviceTokenRepository.findByToken("shared-token").get().getUserId())
            );
        }

        @Test
        @DisplayName("returns 400 when token is blank")
        void blankToken_ReturnsBadRequest() {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/notifications/tokens",
                    request(userId, new SaveTokenRequest("  ", DeviceType.IOS)),
                    String.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
