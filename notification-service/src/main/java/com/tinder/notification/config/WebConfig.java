package com.tinder.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinder.notification.properties.GatewayAuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.security.gateway-auth", name = "enabled", havingValue = "true")
    public FilterRegistrationBean<GatewayAuthFilter> gatewayAuthFilterRegistration(GatewayAuthProperties properties, ObjectMapper objectMapper) {

        FilterRegistrationBean<GatewayAuthFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new GatewayAuthFilter(properties, objectMapper));

        registrationBean.addUrlPatterns("/*");

        registrationBean.setOrder(1);

        return registrationBean;
    }
}