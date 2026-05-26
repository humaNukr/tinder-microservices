package com.tinder.feed.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.feed.dto.ProfileResponse;
import com.tinder.feed.properties.ProfileServiceProperties;
import com.tinder.feed.util.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Feed Controller — Integration Tests")
class FeedControllerIT extends BaseIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileServiceProperties profileProps;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        wireMock.resetAll();
    }

    private HttpEntity<Void> requestWithHeader(UUID id) {
        HttpHeaders headers = new HttpHeaders();
        if (id != null) {
            headers.add("X-User-Id", id.toString());
        }
        return new HttpEntity<>(null, headers);
    }

    private ProfileResponse profile(UUID id, String name) {
        return new ProfileResponse(id, name, 25, "MALE", "Bio", List.of(), List.of());
    }

    @Test
    @DisplayName("GET /api/v1/feed — returns 200 OK and list of profiles when X-User-Id is valid")
    void getFeed_ValidHeader_ReturnsProfiles() throws Exception {
        UUID candidateId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo(profileProps.candidatesPath()))
                .willReturn(okJson(objectMapper.writeValueAsString(List.of(candidateId)))));
        stubFor(post(urlEqualTo(profileProps.batchPath()))
                .willReturn(okJson(objectMapper.writeValueAsString(List.of(profile(candidateId, "Test"))))));

        ResponseEntity<List<ProfileResponse>> response = restTemplate.exchange(
                "/api/v1/feed",
                HttpMethod.GET,
                requestWithHeader(userId),
                new ParameterizedTypeReference<>() {}
        );

        assertAll(
                () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                () -> assertTrue(response.getBody() != null && !response.getBody().isEmpty())
        );
    }

    @Test
    @DisplayName("GET /api/v1/feed — returns 400 Bad Request when X-User-Id is missing")
    void getFeed_MissingHeader_Returns400() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/feed",
                HttpMethod.GET,
                requestWithHeader(null),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/feed — returns 400 Bad Request when X-User-Id is invalid UUID")
    void getFeed_InvalidUUID_Returns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "not-a-uuid");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/feed",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
