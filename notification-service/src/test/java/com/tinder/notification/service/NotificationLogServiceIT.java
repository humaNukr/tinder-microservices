package com.tinder.notification.service;

import com.tinder.notification.dto.NotificationResponseDto;
import com.tinder.notification.entity.Notification;
import com.tinder.notification.enums.NotificationType;
import com.tinder.notification.exception.AccessDeniedException;
import com.tinder.notification.exception.EntityNotFoundException;
import com.tinder.notification.repository.NotificationRepository;
import com.tinder.notification.service.interfaces.NotificationLogService;
import com.tinder.notification.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("NotificationLogService — Integration Tests")
class NotificationLogServiceIT extends BaseIT {

    @Autowired
    private NotificationLogService notificationLogService;

    @Autowired
    private NotificationRepository notificationRepository;

    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        notificationRepository.deleteAll();
    }

    private Notification persistNotification(UUID owner, boolean read) {
        Notification notification = Notification.builder()
                .userId(owner)
                .title("Title")
                .body("Body")
                .type(NotificationType.MESSAGE)
                .metadata(Map.of("key", "value"))
                .isRead(read)
                .build();
        return notificationRepository.save(notification);
    }

    @Nested
    @DisplayName("createNotification()")
    class CreateNotification {

        @Test
        @DisplayName("persists notification with metadata")
        void validPayload_SavedInDatabase() {
            notificationLogService.createNotification(
                    userId,
                    "Hello",
                    "World",
                    NotificationType.MESSAGE,
                    Map.of("chatId", UUID.randomUUID())
            );

            Notification saved = notificationRepository.findAllByUserId(userId, PageRequest.of(0, 1))
                    .getContent().getFirst();

            assertAll(
                    () -> assertEquals(1, notificationRepository.countByUserId(userId)),
                    () -> assertEquals("Hello", saved.getTitle()),
                    () -> assertEquals(NotificationType.MESSAGE, saved.getType()),
                    () -> assertFalse(saved.isRead())
            );
        }
    }

    @Nested
    @DisplayName("getUserNotifications()")
    class GetUserNotifications {

        @Test
        @DisplayName("returns only notifications belonging to the user")
        void multipleUsers_ReturnsOnlyOwned() {
            persistNotification(userId, false);
            persistNotification(otherUserId, false);

            Page<NotificationResponseDto> result = notificationLogService.getUserNotifications(
                    userId,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("returns requested page size")
        void pagination_ReturnsPageSlice() {
            IntStream.range(0, 5).forEach(i -> persistNotification(userId, false));

            Page<NotificationResponseDto> result = notificationLogService.getUserNotifications(
                    userId,
                    PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"))
            );

            assertAll(
                    () -> assertEquals(2, result.getContent().size()),
                    () -> assertEquals(5, result.getTotalElements()),
                    () -> assertEquals(3, result.getTotalPages())
            );
        }
    }

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCount {

        @Test
        @DisplayName("counts only unread notifications for the user")
        void mixedReadState_CountsUnreadOnly() {
            persistNotification(userId, false);
            persistNotification(userId, true);
            persistNotification(otherUserId, false);

            assertEquals(1, notificationLogService.getUnreadCount(userId));
        }
    }

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsRead {

        @Test
        @DisplayName("marks all unread notifications for user")
        void unreadNotifications_AllMarkedRead() {
            persistNotification(userId, false);
            persistNotification(userId, false);
            persistNotification(userId, true);

            notificationLogService.markAllAsRead(userId);

            assertEquals(0, notificationLogService.getUnreadCount(userId));
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("marks notification as read for the owner")
        void owner_MarksAsRead() {
            Notification notification = persistNotification(userId, false);

            notificationLogService.markAsRead(notification.getId(), userId);

            Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
            assertTrue(updated.isRead());
        }

        @Test
        @DisplayName("throws when notification does not exist")
        void unknownId_ThrowsEntityNotFound() {
            assertThrows(
                    EntityNotFoundException.class,
                    () -> notificationLogService.markAsRead(UUID.randomUUID(), userId)
            );
        }

        @Test
        @DisplayName("throws when user is not the owner")
        void wrongUser_ThrowsAccessDenied() {
            Notification notification = persistNotification(userId, false);

            assertThrows(
                    AccessDeniedException.class,
                    () -> notificationLogService.markAsRead(notification.getId(), otherUserId)
            );
        }
    }
}
