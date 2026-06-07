package com.tinder.apigateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class GatewayApplicationIT {

	private final UUID testUserId = UUID.randomUUID();
	@Autowired
	private WebTestClient webClient;

	@Value("${jwt.secret}")
	private String jwtSecret;

	private String validToken;

	@BeforeEach
	void setUp() {
		SecretKey key = Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret));
		validToken = Jwts.builder().subject(testUserId.toString()).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)).signWith(key).compact();

		stubFor(get(urlEqualTo("/api/v1/profiles/me"))
				.willReturn(aResponse().withStatus(200).withBody("{\"status\": \"ok\"}")));
	}

	@Test
	@DisplayName("AuthenticationFilter should reject request without token on secured endpoint")
	void authFilter_MissingToken_Returns401() {
		webClient.get().uri("/api/v1/profiles/me").exchange().expectStatus().isUnauthorized().expectBody()
				.jsonPath("$.error").isEqualTo("Unauthorized").jsonPath("$.message")
				.isEqualTo("Authorization header is missing");
	}

	@Test
	@DisplayName("AuthenticationFilter should allow request to open endpoints without token")
	void authFilter_OpenEndpoint_AllowsWithoutToken() {
		stubFor(get(urlEqualTo("/api/v1/auth/send-otp")).willReturn(aResponse().withStatus(200)));

		webClient.get().uri("/api/v1/auth/send-otp").exchange().expectStatus().isOk();
	}

	@Test
	@DisplayName("AuthenticationFilter should extract userId and pass it as header")
	void authFilter_ValidToken_AddsUserIdHeader() {
		webClient.get().uri("/api/v1/profiles/me").header("Authorization", "Bearer " + validToken).exchange()
				.expectStatus().isOk();

		verify(getRequestedFor(urlEqualTo("/api/v1/profiles/me")).withHeader("X-User-Id",
				equalTo(testUserId.toString())));
	}

	@TestConfiguration
	static class WireMockRouteConfig {
		@Bean
		public RouteLocator wireMockRoutes(RouteLocatorBuilder builder, @Value("${wiremock.server.port}") int port) {
			return builder.routes()
					.route("test-profile-service", r -> r.path("/api/v1/profiles/**").uri("http://localhost:" + port))
					.route("test-auth-service", r -> r.path("/api/v1/auth/**").uri("http://localhost:" + port)).build();
		}
	}
}
