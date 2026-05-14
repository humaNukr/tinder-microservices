package com.tinder.chat.application.service.presence;

import com.tinder.chat.application.port.out.presence.PresenceEventPort;
import com.tinder.chat.application.port.out.presence.PresenceStatePort;
import com.tinder.chat.shared.dto.event.UserPresenceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPresenceManagementServiceTest {

    @Mock
    private PresenceStatePort presenceStatePort;

    @Mock
    private PresenceEventPort presenceEventPort;

    @InjectMocks
    private UserPresenceManagementService userPresenceManagementService;

    @Captor
    private ArgumentCaptor<UserPresenceEvent> eventCaptor;

    private UUID userId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = "session-123";
    }

    @Nested
    class UserConnected {

        @Test
        void userConnected_NewlyOnline_RegistersSessionAndPublishesOnlineEvent() {
            when(presenceStatePort.registerSessionAndClearGrace(userId, sessionId)).thenReturn(true);

            userPresenceManagementService.userConnected(userId, sessionId);

            verify(presenceStatePort).registerSessionAndClearGrace(userId, sessionId);
            verifyOnlineEventPublished();
        }

        @Test
        void userConnected_AlreadyOnline_RegistersSessionAndDoesNotPublishEvent() {
            when(presenceStatePort.registerSessionAndClearGrace(userId, sessionId)).thenReturn(false);

            userPresenceManagementService.userConnected(userId, sessionId);

            verify(presenceStatePort).registerSessionAndClearGrace(userId, sessionId);
            verifyNoEventsPublished();
        }
    }

    @Nested
    class UserDisconnected {

        @Test
        void userDisconnected_NowOffline_UnregistersSessionAndStartsGracePeriod() {
            when(presenceStatePort.unregisterSession(userId, sessionId)).thenReturn(true);

            userPresenceManagementService.userDisconnected(userId, sessionId);

            verify(presenceStatePort).unregisterSession(userId, sessionId);
            verify(presenceStatePort).startGracePeriod(userId);
        }

        @Test
        void userDisconnected_StillOnline_UnregistersSessionAndDoesNotStartGracePeriod() {
            when(presenceStatePort.unregisterSession(userId, sessionId)).thenReturn(false);

            userPresenceManagementService.userDisconnected(userId, sessionId);

            verify(presenceStatePort).unregisterSession(userId, sessionId);
            verify(presenceStatePort, never()).startGracePeriod(userId);
        }
    }

    @Nested
    class HandleGracePeriodExpired {

        @Test
        void handleGracePeriodExpired_UserIsOffline_PublishesOfflineEvent() {
            when(presenceStatePort.isOffline(userId)).thenReturn(true);

            userPresenceManagementService.handleGracePeriodExpired(userId);

            verify(presenceStatePort).isOffline(userId);
            verifyOfflineEventPublished();
        }

        @Test
        void handleGracePeriodExpired_UserIsOnline_DoesNotPublishEvent() {
            when(presenceStatePort.isOffline(userId)).thenReturn(false);

            userPresenceManagementService.handleGracePeriodExpired(userId);

            verify(presenceStatePort).isOffline(userId);
            verifyNoEventsPublished();
        }
    }

    private void verifyOnlineEventPublished() {
        verify(presenceEventPort).broadcastToChatServers(eventCaptor.capture());
        verify(presenceEventPort, never()).broadcastToSystem(any());

        UserPresenceEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.userId());
        assertTrue(capturedEvent.isOnline());
        assertNotNull(capturedEvent.timestamp());
    }

    private void verifyOfflineEventPublished() {
        verify(presenceEventPort).broadcastToChatServers(eventCaptor.capture());
        verify(presenceEventPort).broadcastToSystem(eventCaptor.getValue());

        UserPresenceEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.userId());
        assertFalse(capturedEvent.isOnline());
        assertNotNull(capturedEvent.timestamp());
    }

    private void verifyNoEventsPublished() {
        verify(presenceEventPort, never()).broadcastToChatServers(any());
        verify(presenceEventPort, never()).broadcastToSystem(any());
    }
}