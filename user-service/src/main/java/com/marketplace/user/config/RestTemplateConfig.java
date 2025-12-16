package com.marketplace.user.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate.
 */
@Configuration
public class RestTemplateConfig {

    private final AuthServiceProperties authServiceProperties;

    public RestTemplateConfig(AuthServiceProperties authServiceProperties) {
        this.authServiceProperties = authServiceProperties;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(authServiceProperties.connectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(authServiceProperties.readTimeoutMs()))
                .build();
    }
}
