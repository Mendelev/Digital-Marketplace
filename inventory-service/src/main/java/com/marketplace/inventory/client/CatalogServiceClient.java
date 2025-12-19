package com.marketplace.inventory.client;

import com.marketplace.inventory.config.CatalogServiceProperties;
import com.marketplace.inventory.dto.ProductResponse;
import com.marketplace.inventory.exception.CatalogServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class CatalogServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogServiceClient.class);
    private static final String SERVICE_SECRET_HEADER = "X-Service-Secret";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final RestTemplate restTemplate;
    private final CatalogServiceProperties properties;

    public CatalogServiceClient(RestTemplate restTemplate, CatalogServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @CircuitBreaker(name = "catalogService", fallbackMethod = "getProductFallback")
    public ProductResponse getProduct(UUID productId) {
        String url = properties.baseUrl() + "/api/v1/catalog/products/" + productId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(SERVICE_SECRET_HEADER, properties.sharedSecret());

        // Propagate correlation ID
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            headers.set(CORRELATION_ID_HEADER, correlationId);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            log.debug("Fetching product from Catalog Service: {}", productId);

            ResponseEntity<ProductResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, ProductResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully fetched product: {}", productId);
                return response.getBody();
            } else {
                log.error("Unexpected response from Catalog Service: {}", response.getStatusCode());
                throw new CatalogServiceException("Unexpected response: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            log.error("HTTP client error from Catalog Service: {} - {}", e.getStatusCode(), e.getMessage());
            throw new CatalogServiceException("Catalog service error: " + e.getStatusCode(), e);

        } catch (HttpServerErrorException e) {
            log.error("HTTP server error from Catalog Service: {} - {}", e.getStatusCode(), e.getMessage());
            throw new CatalogServiceException("Catalog service error: " + e.getStatusCode(), e);

        } catch (ResourceAccessException e) {
            log.error("Failed to connect to Catalog Service: {}", e.getMessage());
            throw new CatalogServiceException("Failed to connect to Catalog Service", e);
        }
    }

    @SuppressWarnings("unused")
    private ProductResponse getProductFallback(UUID productId, Exception e) {
        log.error("Circuit breaker fallback triggered for product: {}", productId, e);
        throw new CatalogServiceException("Catalog service unavailable", e);
    }
}
