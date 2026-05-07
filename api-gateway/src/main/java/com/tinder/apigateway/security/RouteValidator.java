package com.tinder.apigateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify",
            "/api/v1/auth/google",
            "/ws/chat",
            "/media/"
    );

    public static final List<String> securedApiEndpoints = List.of(
            "/api/v1/auth/me",
            "/api/v1/auth/logout"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();

                if (securedApiEndpoints.stream().anyMatch(path::startsWith)) {
                    return true;
                }

                if (path.startsWith("/api/v1/auth") && securedApiEndpoints.stream().noneMatch(path::startsWith)) {
                    return false;
                }

                return openApiEndpoints.stream().noneMatch(path::startsWith);
            };
}