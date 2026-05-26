package com.tinder.chat.application.service.account;

import com.tinder.chat.domain.enums.ActivityType;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventEntity;
import com.tinder.chat.infrastructure.adapter.out.persistence.inbox.InboxEventJpaRepository;
import com.tinder.chat.shared.dto.event.UserActivityEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountDeletionProcessor")
class AccountDeletionProcessorTest {

    @Mock
    private InboxEventJpaRepository inboxEventRepository;

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

        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);

        processor.process(event);

        verify(inboxEventRepository).save(any(InboxEventEntity.class));
        verify(accountDeletionService).deleteUserData(userId);
    }

    @Test
    @DisplayName("skips deletion for duplicate event")
    void process_DuplicateEvent_SkipsDeletion() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserActivityEvent event =
                new UserActivityEvent(eventId, userId, ActivityType.DELETE_ACCOUNT, Instant.now());

        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(true);

        processor.process(event);

        verify(inboxEventRepository, never()).save(any());
        verify(accountDeletionService, never()).deleteUserData(userId);
    }
}
