package com.tinder.feed.config;

import com.tinder.feed.properties.ServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class RestClientConfig {

    private final ServiceProperties serviceProperties;

    @Bean
    public RestClient customRestClient() {
        return RestClient.builder()
                .baseUrl(serviceProperties.profileUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }
}