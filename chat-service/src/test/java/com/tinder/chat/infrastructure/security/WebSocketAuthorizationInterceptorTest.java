package com.tinder.chat.infrastructure.security;

import com.tinder.chat.domain.exception.AccessDeniedException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthorizationInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketAuthorizationInterceptor interceptor;

    private Message<?> createMessage(StompCommand command, String token, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (token != null) {
            accessor.setNativeHeader("Authorization", "Bearer " + token);
        }
        if (principal != null) {
            accessor.setUser(principal);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Nested
    class ConnectCommand {

        @Test
        void preSend_ConnectWithValidToken_SetsUserPrincipal() {
            String token = "valid.jwt.token";
            String userId = UUID.randomUUID().toString();
            Message<?> message = createMessage(StompCommand.CONNECT, token, null);

            when(jwtUtil.extractUserId(token)).thenReturn(userId);

            Message<?> result = interceptor.preSend(message, messageChannel);

            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            Principal principal = accessor.getUser();

            assertNotNull(principal);
            assertEquals(userId, principal.getName());
            verify(jwtUtil).extractUserId(token);
        }

        @Test
        void preSend_ConnectWithMissingToken_ThrowsAccessDeniedException() {
            Message<?> message = createMessage(StompCommand.CONNECT, null, null);

            AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                    interceptor.preSend(message, messageChannel)
            );

            assertEquals("Missing JWT token", exception.getMessage());
        }

        @Test
        void preSend_ConnectWithInvalidToken_ThrowsAccessDeniedException() {
            String token = "invalid.token";
            Message<?> message = createMessage(StompCommand.CONNECT, token, null);

            when(jwtUtil.extractUserId(token)).thenThrow(new JwtException("Expired token"));

            AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                    interceptor.preSend(message, messageChannel)
            );

            assertEquals("Invalid JWT token", exception.getMessage());
        }
    }

    @Nested
    class ProtectedCommands {

        @Test
        void preSend_SubscribeWithAuthenticatedUser_AllowsMessage() {
            Principal principal = new StompPrincipal(UUID.randomUUID().toString());
            Message<?> message = createMessage(StompCommand.SUBSCRIBE, null, principal);

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertEquals(message, result);
        }

        @Test
        void preSend_SubscribeWithUnauthenticatedUser_ThrowsAccessDeniedException() {
            Message<?> message = createMessage(StompCommand.SUBSCRIBE, null, null);

            AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                    interceptor.preSend(message, messageChannel)
            );

            assertEquals("Unauthenticated WebSocket message", exception.getMessage());
        }

        @Test
        void preSend_SendWithAuthenticatedUser_AllowsMessage() {
            Principal principal = new StompPrincipal(UUID.randomUUID().toString());
            Message<?> message = createMessage(StompCommand.SEND, null, principal);

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertEquals(message, result);
        }

        @Test
        void preSend_SendWithUnauthenticatedUser_ThrowsAccessDeniedException() {
            Message<?> message = createMessage(StompCommand.SEND, null, null);

            assertThrows(AccessDeniedException.class, () ->
                    interceptor.preSend(message, messageChannel)
            );
        }
    }

    @Nested
    class IgnoredCommands {

        @Test
        void preSend_NullCommand_AllowsMessage() {
            Message<?> message = MessageBuilder.withPayload(new byte[0]).build();

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertEquals(message, result);
        }

        @Test
        void preSend_OtherCommand_AllowsMessage() {
            Principal principal = new StompPrincipal(UUID.randomUUID().toString());
            Message<?> message = createMessage(StompCommand.DISCONNECT, null, principal);

            Message<?> result = interceptor.preSend(message, messageChannel);

            assertEquals(message, result);
        }
    }
}