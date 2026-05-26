package com.tinder.profile.processor;

import com.tinder.profile.event.ActivityType;
import com.tinder.profile.event.UserActivityEvent;
import com.tinder.profile.service.impl.InboxDedupService;
import com.tinder.profile.service.interfaces.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserActivityProcessor")
class UserActivityProcessorTest {

    @Mock
    private InboxDedupService inboxDedupService;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private UserActivityProcessor processor;

    @Nested
    @DisplayName("deleteProfileData()")
    class DeleteProfileData {

        @Test
        @DisplayName("claims inbox and deletes account data on first delivery")
        void firstDelivery_DeletesAccount() {
            UUID eventId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserActivityEvent event = new UserActivityEvent(
                    eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());

            when(inboxDedupService.tryRegister(eventId)).thenReturn(true);

            processor.deleteProfileData(event);

            verify(profileService).deleteAccountData(userId);
        }

        @Test
        @DisplayName("skips processing when event was already registered")
        void duplicateDelivery_SkipsDelete() {
            UUID eventId = UUID.randomUUID();
            UserActivityEvent event = new UserActivityEvent(
                    eventId, UUID.randomUUID(), ActivityType.DELETE_ACCOUNT, Instant.now());

            when(inboxDedupService.tryRegister(eventId)).thenReturn(false);

            processor.deleteProfileData(event);

            verify(profileService, never()).deleteAccountData(event.userId());
        }
    }
}
