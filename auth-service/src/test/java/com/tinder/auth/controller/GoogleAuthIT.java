package com.tinder.auth.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.tinder.auth.AuthServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AuthServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"jwt.secret=84356789345678934567893456789456789456789456789", // Будь-який довгий рядок
		"app.google.client-id=test-client-id"})
@AutoConfigureMockMvc
@Testcontainers
public class GoogleAuthIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
			.withDatabaseName("auth_test_db").withUsername("test_user").withPassword("test_pass");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleIdTokenVerifier googleIdTokenVerifier;

	@Test
	void authenticateWithGoogle_ShouldReturnTokens_WhenTokenIsValid() throws Exception {
		GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
		payload.setEmail("test.senior@gmail.com");
		payload.setEmailVerified(true);

		JsonWebSignature.Header header = new JsonWebSignature.Header();
		byte[] signature = new byte[0];
		GoogleIdToken fakeToken = new GoogleIdToken(header, payload, signature, signature);

		when(googleIdTokenVerifier.verify(anyString())).thenReturn(fakeToken);

		String requestBody = """
				{
				    "idToken": "some-fake-jwt-string"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/google").header("X-Device-Id", "test-device-uuid")
				.contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists()).andExpect(jsonPath("$.refreshToken").exists());
	}
}
