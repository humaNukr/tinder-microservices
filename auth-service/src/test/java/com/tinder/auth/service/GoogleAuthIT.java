package com.tinder.auth.service;

import com.tinder.auth.service.impl.GoogleTokenVerifier;
import com.tinder.auth.util.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GoogleAuthIT extends BaseIT {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleTokenVerifier googleTokenVerifier;

	@Test
	void authenticateWithGoogle_ShouldReturnTokens_WhenTokenIsValid() throws Exception {
		when(googleTokenVerifier.verifyTokenAndGetEmail(anyString())).thenReturn("test.senior@gmail.com");
		when(googleTokenVerifier.getSupportedProvider()).thenCallRealMethod();
		when(googleTokenVerifier.verifyTokenAndGetIdentifier(anyString())).thenCallRealMethod();

		String requestBody = """
				{
				    "token": "some-fake-jwt-string"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/oauth/google").header("X-Device-Id", "test-device-uuid")
				.contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists()).andExpect(jsonPath("$.refreshToken").exists());
	}
}
