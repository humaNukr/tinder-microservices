package com.tinder.notification.controller;

import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.dto.UnreadCountResponse;
import com.tinder.notification.dto.error.ErrorResponseDto;
import com.tinder.notification.entity.Notification;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.repository.NotificationRepository;
import com.tinder.notification.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Notification Controller — Integration Tests")
class NotificationControllerIT extends BaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationRepository.deleteAll();
    }

    private HttpEntity<Void> requestWithUserId(UUID id) {
        HttpHeaders headers = new HttpHeaders();
        if (id != null) {
            headers.add("X-User-Id", id.toString());
        }
        return new HttpEntity<>(null, headers);
    }

    private Notification seedNotification(UUID owner, boolean read, String title) {
        return notificationRepository.save(Notification.builder()
                .userId(owner)
                .title(title)
                .body("Body")
                .type(NotificationType.MESSAGE)
                .metadata(Map.of())
                .isRead(read)
                .build());
    }

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetNotifications {

        @Test
        @DisplayName("returns 200 and paginated notifications")
        void validUser_ReturnsPagedNotifications() {
            IntStream.range(0, 3).forEach(i -> seedNotification(userId, false, "N" + i));

            ResponseEntity<RestPage<NotificationResponseDto>> response = restTemplate.exchange(
                    "/api/v1/notifications?page=0&size=2",
                    HttpMethod.GET,
                    requestWithUserId(userId),
                    new ParameterizedTypeReference<>() {}
            );

            assertAll(
                    () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                    () -> assertEquals(2, response.getBody().content().size()),
                    () -> assertEquals(3, response.getBody().totalElements())
            );
        }

        @Test
        @DisplayName("returns empty page when user has no notifications")
        void noNotifications_ReturnsEmptyPage() {
            ResponseEntity<RestPage<NotificationResponseDto>> response = restTemplate.exchange(
                    "/api/v1/notifications",
                    HttpMethod.GET,
                    requestWithUserId(userId),
                    new ParameterizedTypeReference<>() {}
            );

            assertTrue(response.getBody().content().isEmpty());
        }

        @Test
        @DisplayName("returns 400 when X-User-Id is invalid UUID")
        void invalidUserId_Returns400() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-User-Id", "invalid");
            ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                    "/api/v1/notifications",
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    ErrorResponseDto.class
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/unread-count")
    class UnreadCount {

        @Test
        @DisplayName("returns count of unread notifications")
        void unreadExist_ReturnsCount() {
            seedNotification(userId, false, "Unread");
            seedNotification(userId, true, "Read");

            ResponseEntity<UnreadCountResponse> response = restTemplate.exchange(
                    "/api/v1/notifications/unread-count",
                    HttpMethod.GET,
                    requestWithUserId(userId),
                    UnreadCountResponse.class
            );

            assertAll(
                    () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                    () -> assertEquals(1, response.getBody().unreadCount())
            );
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("returns 204 and marks all unread notifications as read")
        void owner_ReturnsNoContent() {
            seedNotification(userId, false, "One");
            seedNotification(userId, false, "Two");

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/notifications/read-all",
                    HttpMethod.PATCH,
                    requestWithUserId(userId),
                    Void.class
            );

            assertAll(
                    () -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode()),
                    () -> assertEquals(0, notificationRepository.countByUserIdAndIsReadFalse(userId))
            );
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/notifications/{id}/read")
    class MarkAsRead {

        @Test
        @DisplayName("returns 204 and marks notification as read")
        void owner_ReturnsNoContent() {
            Notification notification = seedNotification(userId, false, "To read");

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/notifications/" + notification.getId() + "/read",
                    HttpMethod.PATCH,
                    requestWithUserId(userId),
                    Void.class
            );

            Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();

            assertAll(
                    () -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode()),
                    () -> assertTrue(updated.isRead())
            );
        }

        @Test
        @DisplayName("returns 403 when user does not own notification")
        void wrongUser_Returns403() {
            Notification notification = seedNotification(userId, false, "Owned");
            UUID otherUser = UUID.randomUUID();

            ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
                    "/api/v1/notifications/" + notification.getId() + "/read",
                    HttpMethod.PATCH,
                    requestWithUserId(otherUser),
                    ErrorResponseDto.class
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    private record RestPage<T>(
            java.util.List<T> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }
}
