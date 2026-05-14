package com.tinder.chat.infrastructure.security;

import com.tinder.chat.domain.exception.AccessDeniedException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthorizationInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String token = authHeaders.getFirst().replace("Bearer ", "");

                try {
                    String userId = jwtUtil.extractUserId(token);

                    Principal principal = new StompPrincipal(userId);
                    accessor.setUser(principal);

                    log.debug("User {} successfully authenticated in WebSocket", userId);
                } catch (JwtException e) {
                    log.error("Invalid JWT token during WebSocket handshake: {}", e.getMessage());
                    throw new AccessDeniedException("Invalid JWT token");
                }
            } else {
                log.warn("Missing Authorization header in STOMP CONNECT");
                throw new AccessDeniedException("Missing JWT token");
            }
        }

        if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            Principal principal = accessor.getUser();

            if (principal == null) {
                log.warn("Security blocked unauthenticated STOMP command: {}", command);
                throw new AccessDeniedException("Unauthenticated WebSocket message");
            }
        }

        return message;
    }
}