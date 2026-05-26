package com.tinder.notification.processor;

import com.tinder.notification.event.ActivityType;
import com.tinder.notification.event.UserActivityEvent;
import com.tinder.notification.service.AccountDeletionService;
import com.tinder.notification.service.impl.InboxDedupService;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("AccountDeletionProcessor")
class AccountDeletionProcessorTest {

    @Mock
    private InboxDedupService inboxDedupService;

    @Mock
    private AccountDeletionService accountDeletionService;

    @InjectMocks
    private AccountDeletionProcessor processor;

    @Test
    @DisplayName("deletes user data when event is new")
    void process_NewEvent_DeletesData() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserActivityEvent event =
                new UserActivityEvent(eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());

        when(inboxDedupService.tryRegister(eventId)).thenReturn(true);

        processor.process(event);

        verify(accountDeletionService).deleteUserData(userId);
    }

    @Test
    @DisplayName("skips deletion for duplicate event")
    void process_DuplicateEvent_SkipsDeletion() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserActivityEvent event =
                new UserActivityEvent(eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());

        when(inboxDedupService.tryRegister(eventId)).thenReturn(false);

        processor.process(event);

        verify(accountDeletionService, never()).deleteUserData(userId);
    }
}
