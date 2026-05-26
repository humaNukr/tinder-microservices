package com.tinder.profile.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.profile.properties.GatewayAuthProperties;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayAuthFilter")
class GatewayAuthFilterTest {

    private static final String GATEWAY_HEADER = "X-Gateway-Authenticated";
    private static final String USER_HEADER = "X-User-Id";

    @Mock
    private FilterChain filterChain;

    private GatewayAuthFilter filter;

    @BeforeEach
    void setUp() {
        GatewayAuthProperties properties = new GatewayAuthProperties(
                true,
                GATEWAY_HEADER,
                "true",
                USER_HEADER,
                "/api/v1/internal/",
                null
        );
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new GatewayAuthFilter(properties, objectMapper);
    }

    @Nested
    @DisplayName("doFilterInternal()")
    class DoFilterInternal {

        @Test
        @DisplayName("continues filter chain when gateway and user headers are valid")
        void validHeaders_ContinuesChain() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profiles/me");
            request.addHeader(GATEWAY_HEADER, "true");
            request.addHeader(USER_HEADER, "550e8400-e29b-41d4-a716-446655440000");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertEquals(200, response.getStatus());
        }

        @Test
        @DisplayName("returns 403 when gateway header is missing")
        void missingGatewayHeader_Returns403() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profiles/me");
            request.addHeader(USER_HEADER, "550e8400-e29b-41d4-a716-446655440000");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertEquals(403, response.getStatus());
        }

        @Test
        @DisplayName("returns 401 when user id header is missing")
        void missingUserHeader_Returns401() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/profiles/me");
            request.addHeader(GATEWAY_HEADER, "true");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(request, response);
            assertEquals(401, response.getStatus());
        }

        @Test
        @DisplayName("skips internal API paths for service-to-service calls")
        void internalPath_SkipsFilter() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest(
                    "GET", "/api/v1/internal/profiles/candidates");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }
}
