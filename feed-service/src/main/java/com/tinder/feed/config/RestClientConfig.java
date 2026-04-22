package com.tinder.feed.config;

import com.tinder.feed.adapter.ProfileClient;
import com.tinder.feed.properties.ProfileServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final ProfileServiceProperties serviceProperties;

    @Bean
    public ProfileClient profileClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl(serviceProperties.url())
                .defaultHeader("Accept", "application/json")
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(ProfileClient.class);
    }
}