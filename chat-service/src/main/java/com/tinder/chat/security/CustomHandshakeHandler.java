package com.tinder.chat.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        String userId = request.getHeaders().getFirst("X-User-Id");

        //for simplify tests. Can be deleted later
        if (userId == null || userId.isBlank()) {
            userId = UriComponentsBuilder.fromUri(request.getURI())
                    .build()
                    .getQueryParams()
                    .getFirst("userId");
        }
        //

        if (userId == null || userId.isBlank()) {
            return null;
        }

        return new StompPrincipal(userId);
    }
}