package com.tinder.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.notification.properties.GatewayAuthProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayAuthFilter")
class GatewayAuthFilterTest {

    @Mock
    private FilterChain filterChain;

    private GatewayAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        GatewayAuthProperties properties = new GatewayAuthProperties(
                true,
                "X-Gateway-Authenticated",
                "true",
                "X-User-Id"
        );
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new GatewayAuthFilter(properties, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRequestURI("/api/v1/notifications");
    }

    @Nested
    @DisplayName("doFilterInternal()")
    class DoFilterInternal {

        @Test
        @DisplayName("returns 403 when gateway header is missing")
        void missingGatewayHeader_Returns403() throws Exception {
            request.addHeader("X-User-Id", UUID.randomUUID().toString());

            filter.doFilter(request, response, filterChain);

            assertEquals(403, response.getStatus());
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("returns 401 when user id header is missing")
        void missingUserId_Returns401() throws Exception {
            request.addHeader("X-Gateway-Authenticated", "true");

            filter.doFilter(request, response, filterChain);

            assertEquals(401, response.getStatus());
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("continues filter chain when gateway and user headers are valid")
        void validHeaders_ContinuesChain() throws Exception {
            request.addHeader("X-Gateway-Authenticated", "true");
            request.addHeader("X-User-Id", UUID.randomUUID().toString());

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("skips non-api paths")
        void actuatorPath_SkipsFilter() throws Exception {
            request.setRequestURI("/actuator/health");

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
