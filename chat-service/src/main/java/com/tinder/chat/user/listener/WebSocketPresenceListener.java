package com.tinder.chat.user.listener;

import com.tinder.chat.user.service.UserPresenceServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketPresenceListener {

    private final UserPresenceServiceImpl presenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        
        if (principal != null) {
            UUID userId = UUID.fromString(principal.getName());
            String sessionId = headerAccessor.getSessionId();
            log.info("🟢 User Connected: {} (Session: {})", userId, sessionId);
            presenceService.userConnected(userId, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        
        if (principal != null) {
            UUID userId = UUID.fromString(principal.getName());
            String sessionId = headerAccessor.getSessionId();
            log.info("🔴 User Disconnected: {} (Session: {})", userId, sessionId);
            presenceService.userDisconnected(userId, sessionId);
        }
    }
}