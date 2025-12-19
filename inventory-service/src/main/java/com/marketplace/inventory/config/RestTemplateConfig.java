package com.marketplace.inventory.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    private final CatalogServiceProperties catalogProperties;

    public RestTemplateConfig(CatalogServiceProperties catalogProperties) {
        this.catalogProperties = catalogProperties;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(catalogProperties.connectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(catalogProperties.readTimeoutMs()))
                .build();
    }
}
