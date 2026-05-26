package com.tinder.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tinder.auth.properties.GatewayAuthProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
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

	@BeforeEach
	void setUp() {
		GatewayAuthProperties properties = new GatewayAuthProperties(true, "X-Gateway-Authenticated", "true",
				"X-User-Id", null,
				List.of("/api/v1/auth/send-otp", "/api/v1/auth/verify", "/api/v1/auth/refresh", "/api/v1/auth/oauth"));
		filter = new GatewayAuthFilter(properties, new ObjectMapper().registerModule(new JavaTimeModule()));
	}

	@Test
	@DisplayName("skips filter for public auth endpoints")
	void publicAuthPath_SkipsFilter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/send-otp");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
	}

	@Test
	@DisplayName("requires gateway headers for logout")
	void logout_MissingGatewayHeader_Returns403() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
		request.addHeader("X-User-Id", UUID.randomUUID().toString());
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(filterChain, never()).doFilter(request, response);
		assertEquals(403, response.getStatus());
	}
}
