package com.tinder.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.notification.dto.error.ErrorResponseDto;
import com.tinder.notification.properties.GatewayAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class GatewayAuthFilter extends OncePerRequestFilter {

    private final GatewayAuthProperties gatewayAuthProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!gatewayAuthProperties.enabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isGatewayAuthenticated(request)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Request must come through API Gateway");
            return;
        }

        String userId = request.getHeader(gatewayAuthProperties.userIdHeader());
        if (userId == null || userId.isBlank()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authenticated user id is missing");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isGatewayAuthenticated(HttpServletRequest request) {
        String value = request.getHeader(gatewayAuthProperties.headerName());
        return gatewayAuthProperties.expectedValue().equals(value);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponseDto body = new ErrorResponseDto(
                LocalDateTime.now(),
                status,
                HttpServletResponse.SC_FORBIDDEN == status ? "Forbidden" : "Unauthorized",
                message,
                null
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
