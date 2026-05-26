package com.tinder.profile.service;

import com.tinder.profile.repository.InboxEventRepository;
import com.tinder.profile.service.impl.InboxDedupService;
import com.tinder.profile.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InboxDedupService — Integration Tests")
class InboxDedupServiceIT extends BaseIT {

    @Autowired
    private InboxDedupService inboxDedupService;

    @Autowired
    private InboxEventRepository inboxEventRepository;

    private UUID eventId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        inboxEventRepository.deleteAll();
    }

    @Nested
    @DisplayName("tryRegister()")
    class TryRegister {

        @Test
        @DisplayName("returns true and persists event on first registration")
        void firstCall_PersistsAndReturnsTrue() {
            boolean registered = inboxDedupService.tryRegister(eventId);

            assertAll(
                    () -> assertTrue(registered),
                    () -> assertTrue(inboxEventRepository.existsById(eventId)),
                    () -> assertEquals(1, inboxEventRepository.count())
            );
        }

        @Test
        @DisplayName("different event ids are registered independently")
        void distinctEventIds_BothPersisted() {
            UUID otherEventId = UUID.randomUUID();

            assertAll(
                    () -> assertTrue(inboxDedupService.tryRegister(eventId)),
                    () -> assertTrue(inboxDedupService.tryRegister(otherEventId)),
                    () -> assertEquals(2, inboxEventRepository.count())
            );
        }

        @Test
        @DisplayName("returns false on duplicate without creating second row")
        void duplicateCall_ReturnsFalse() {
            assertTrue(inboxDedupService.tryRegister(eventId));

            boolean secondAttempt = inboxDedupService.tryRegister(eventId);

            assertAll(
                    () -> assertFalse(secondAttempt),
                    () -> assertEquals(1, inboxEventRepository.count())
            );
        }
    }
}
