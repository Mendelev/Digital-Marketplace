package com.marketplace.shipping.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    private final OrderServiceProperties orderServiceProperties;

    public RestTemplateConfig(OrderServiceProperties orderServiceProperties) {
        this.orderServiceProperties = orderServiceProperties;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofMillis(orderServiceProperties.connectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(orderServiceProperties.readTimeoutMs()))
            .build();
    }
}
