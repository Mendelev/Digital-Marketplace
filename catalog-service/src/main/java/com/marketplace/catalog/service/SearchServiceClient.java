package com.marketplace.catalog.service;

import com.marketplace.catalog.domain.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.marketplace.shared.dto.catalog.ProductSearchDocument;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;

@Service
public class SearchServiceClient {
    
    private static final Logger log = LoggerFactory.getLogger(SearchServiceClient.class);
    
    private final WebClient webClient;
    
    @Value("${search-service.base-url}")
    private String searchServiceBaseUrl;
    
    public SearchServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    @CircuitBreaker(name = "searchService", fallbackMethod = "indexProductFallback")
    public void indexProduct(Product product) {
        try {
            webClient.post()
                .uri(searchServiceBaseUrl + "/api/v1/search/index/product")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toSearchDocument(product))
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .block();
            
            log.info("Indexed product {} in search service", product.getId());
        } catch (Exception e) {
            log.error("Failed to index product {} in search service: {}", product.getId(), e.getMessage());
            throw e; // Re-throw to trigger circuit breaker
        }
    }
    
    @CircuitBreaker(name = "searchService", fallbackMethod = "updateProductFallback")
    public void updateProduct(Product product) {
        try {
            webClient.put()
                .uri(searchServiceBaseUrl + "/api/v1/search/index/product/" + product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(toSearchDocument(product))
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .block();
            
            log.info("Updated product {} in search service", product.getId());
        } catch (Exception e) {
            log.error("Failed to update product {} in search service: {}", product.getId(), e.getMessage());
            throw e; // Re-throw to trigger circuit breaker
        }
    }
    
    @CircuitBreaker(name = "searchService", fallbackMethod = "deleteProductFallback")
    public void deleteProductFromIndex(UUID productId) {
        try {
            webClient.delete()
                .uri(searchServiceBaseUrl + "/api/v1/search/index/product/" + productId)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .block();
            
            log.info("Deleted product {} from search service", productId);
        } catch (Exception e) {
            log.error("Failed to delete product {} from search service: {}", productId, e.getMessage());
            throw e; // Re-throw to trigger circuit breaker
        }
    }
    
    // Fallback methods - log error but don't fail the main operation
    private void indexProductFallback(Product product, Throwable t) {
        log.error("Circuit breaker activated: Failed to index product {} in search service: {}", 
            product.getId(), t.getMessage());
        // Could queue for retry or emit event for async processing
    }
    
    private void updateProductFallback(Product product, Throwable t) {
        log.error("Circuit breaker activated: Failed to update product {} in search service: {}", 
            product.getId(), t.getMessage());
    }
    
    private void deleteProductFallback(UUID productId, Throwable t) {
        log.error("Circuit breaker activated: Failed to delete product {} from search service: {}", 
            productId, t.getMessage());
    }
    
    private ProductSearchDocument toSearchDocument(Product product) {
        return new ProductSearchDocument(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getBasePrice(),
            product.getCategory().getName(),
            product.getSellerId(),
            product.getStatus().toString(),
            product.getAvailableSizes(),
            product.getAvailableColors(),
            product.getImageUrls() != null && product.getImageUrls().length > 0 
                ? product.getImageUrls()[0] 
                : null
        );
    }
}
