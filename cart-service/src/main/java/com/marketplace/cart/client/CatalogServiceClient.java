package com.marketplace.cart.client;

import com.marketplace.cart.config.CatalogServiceProperties;
import com.marketplace.cart.exception.CatalogServiceException;
import com.marketplace.cart.exception.ProductNotActiveException;
import com.marketplace.cart.exception.ProductNotFoundException;
import com.marketplace.shared.dto.catalog.ProductResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Client for fetching product information from Catalog Service.
 */
@Component
public class CatalogServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogServiceClient.class);
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final RestTemplate restTemplate;
    private final CatalogServiceProperties properties;

    public CatalogServiceClient(RestTemplate restTemplate, CatalogServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Get product by ID with circuit breaker.
     * Validates product exists and is ACTIVE.
     */
    @CircuitBreaker(name = "catalogService", fallbackMethod = "getProductFallback")
    public ProductResponse getProductById(UUID productId) {
        try {
            String url = properties.baseUrl() + "/api/v1/products/" + productId;
            log.debug("Fetching product from catalog: {}", url);

            ProductResponse product = restTemplate.getForObject(url, ProductResponse.class);

            if (product == null) {
                throw new ProductNotFoundException("Product not found: " + productId);
            }

            // Validate product is ACTIVE
            if (!ACTIVE_STATUS.equalsIgnoreCase(product.status())) {
                log.warn("Product {} is not active, status: {}", productId, product.status());
                throw new ProductNotActiveException("Product is not available for purchase: " + productId);
            }

            log.debug("Product fetched successfully: {}", productId);
            return product;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product not found in catalog: {}", productId);
            throw new ProductNotFoundException("Product not found: " + productId);
        } catch (HttpClientErrorException e) {
            log.error("Client error fetching product {}: {}", productId, e.getMessage());
            throw new CatalogServiceException("Failed to fetch product from catalog", e);
        } catch (Exception e) {
            log.error("Error fetching product from catalog: {}", productId, e);
            throw new CatalogServiceException("Catalog service unavailable", e);
        }
    }

    /**
     * Fallback method for circuit breaker.
     */
    @SuppressWarnings("unused")
    private ProductResponse getProductFallback(UUID productId, Exception e) {
        log.error("Circuit breaker activated for product {}", productId, e);
        throw new CatalogServiceException("Catalog service is temporarily unavailable", e);
    }
}
