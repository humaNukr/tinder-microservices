package com.tinder.auth.service;

import com.tinder.auth.dto.user.UserResult;
import com.tinder.auth.entity.User;
import com.tinder.auth.event.ActivityType;
import com.tinder.auth.event.UserActivityEvent;
import com.tinder.auth.exception.UserNotFoundException;
import com.tinder.auth.properties.KafkaProperties;
import com.tinder.auth.repository.UserRepository;
import com.tinder.auth.service.impl.UserServiceImpl;
import com.tinder.auth.service.interfaces.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private KafkaProperties kafkaProperties;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", userId);
        ReflectionTestUtils.setField(testUser, "email", email);
        ReflectionTestUtils.setField(testUser, "authProvider", User.AuthProvider.EMAIL_OTP);
    }

    @Nested
    @DisplayName("findOrCreateUser() Tests")
    class FindOrCreateUserTests {

        @Test
        void findOrCreateUser_UserExists_ReturnsExistingUser() {
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            UserResult result = userService.findOrCreateUser(email, User.AuthProvider.EMAIL_OTP);

            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(testUser, result.user()),
                    () -> assertFalse(result.isNew()),
                    () -> verify(userRepository, never()).save(any(User.class))
            );
        }

        @Test
        void findOrCreateUser_UserDoesNotExist_CreatesAndReturnsNewUser() {
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResult result = userService.findOrCreateUser(email, User.AuthProvider.EMAIL_OTP);

            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(testUser, result.user()),
                    () -> assertTrue(result.isNew()),
                    () -> verify(userRepository).save(any(User.class))
            );
        }

        @Test
        void findOrCreateUser_NewGoogleUser_InvokesCorrectFactory() {
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.findOrCreateUser(email, User.AuthProvider.GOOGLE);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertAll(
                    () -> assertEquals(email, savedUser.getEmail()),
                    () -> assertEquals(User.AuthProvider.GOOGLE, savedUser.getAuthProvider()),
                    () -> assertTrue(savedUser.isEmailVerified())
            );
        }

        @Test
        void findOrCreateUser_RaceConditionDetected_RecoversAndReturnsExistingUser() {
            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(testUser));

            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

            UserResult result = userService.findOrCreateUser(email, User.AuthProvider.EMAIL_OTP);

            assertAll(
                    () -> assertNotNull(result),
                    () -> assertEquals(testUser, result.user()),
                    () -> assertFalse(result.isNew())
            );
        }

        @Test
        void findOrCreateUser_RaceConditionDetectedButRecoveryFails_ThrowsIllegalStateException() {
            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.empty());

            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

            assertThrows(IllegalStateException.class,
                    () -> userService.findOrCreateUser(email, User.AuthProvider.EMAIL_OTP));
        }
    }

    @Nested
    @DisplayName("findUserById() Tests")
    class FindUserByIdTests {

        @Test
        void findUserById_UserExists_ReturnsUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            User result = userService.findUserById(userId);

            assertEquals(testUser, result);
        }

        @Test
        void findUserById_UserDoesNotExist_ThrowsUserNotFoundException() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.findUserById(userId));
        }
    }

    @Nested
    @DisplayName("deleteUser() Tests")
    class DeleteUserTests {

        @Test
        void deleteUser_UserExists_DeletesUserAndSavesOutboxEvent() {
            String expectedTopic = "user-activity-topic";
            when(userRepository.existsById(userId)).thenReturn(true);
            when(kafkaProperties.userActivity()).thenReturn(expectedTopic);

            userService.deleteUser(userId);

            ArgumentCaptor<UserActivityEvent> eventCaptor = ArgumentCaptor.forClass(UserActivityEvent.class);

            assertAll(
                    () -> verify(userRepository).deleteById(userId),
                    () -> verify(outboxService).saveEvent(eq(expectedTopic), eventCaptor.capture()),
                    () -> {
                        UserActivityEvent capturedEvent = eventCaptor.getValue();
                        assertEquals(userId, capturedEvent.userId());
                        assertEquals(ActivityType.DELETE_ACCOUNT, capturedEvent.type());
                        assertNotNull(capturedEvent.eventId());
                        assertNotNull(capturedEvent.timestamp());
                    }
            );
        }

        @Test
        void deleteUser_UserDoesNotExist_ThrowsUserNotFoundException() {
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThrows(UserNotFoundException.class, () -> userService.deleteUser(userId));

            verify(userRepository, never()).deleteById(any());
            verifyNoInteractions(outboxService, kafkaProperties);
        }
    }
}