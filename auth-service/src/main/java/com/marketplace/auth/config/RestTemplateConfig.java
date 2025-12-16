package com.marketplace.auth.config;

import com.marketplace.auth.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

/**
 * Configuration for RestTemplate with correlation ID propagation.
 */
@Configuration
public class RestTemplateConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final UserServiceProperties userServiceProperties;

    public RestTemplateConfig(UserServiceProperties userServiceProperties) {
        this.userServiceProperties = userServiceProperties;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(userServiceProperties.connectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(userServiceProperties.readTimeoutMs()))
                .interceptors(correlationIdInterceptor())
                .build();
    }

    /**
     * Interceptor to propagate correlation ID to downstream services.
     */
    private ClientHttpRequestInterceptor correlationIdInterceptor() {
        return (HttpRequest request, byte[] body, ClientHttpRequestExecution execution) -> {
            String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
            if (correlationId != null) {
                request.getHeaders().set(CORRELATION_ID_HEADER, correlationId);
            }
            return execution.execute(request, body);
        };
    }
}
