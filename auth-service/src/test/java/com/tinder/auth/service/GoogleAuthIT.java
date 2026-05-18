package com.tinder.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
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
