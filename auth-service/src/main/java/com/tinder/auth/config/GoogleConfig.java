package com.tinder.auth.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tinder.auth.properties.GoogleProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
public class GoogleConfig {

	private final GoogleProperties properties;

	@Bean
	public com.google.api.client.http.HttpTransport httpTransport() {
		return new NetHttpTransport();
	}

	@Bean
	public com.google.api.client.json.JsonFactory jsonFactory() {
		return new GsonFactory();
	}

	@Bean
	public GoogleIdTokenVerifier googleIdTokenVerifier(
			com.google.api.client.http.HttpTransport transport,
			com.google.api.client.json.JsonFactory jsonFactory) {
		return new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
				.setAudience(Collections.singletonList(properties.clientId())).build();
	}
}
