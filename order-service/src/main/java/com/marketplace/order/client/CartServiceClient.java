package com.marketplace.order.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketplace.order.config.CartServiceProperties;
import com.marketplace.order.exception.CartServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Client for communicating with Cart Service.
 */
@Component
public class CartServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CartServiceClient.class);

    private final RestTemplate restTemplate;
    private final CartServiceProperties properties;

    public CartServiceClient(RestTemplate restTemplate, CartServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Get cart by cart ID (internal API).
     *
     * @param cartId Cart ID
     * @return CartResponse
     * @throws CartServiceException if communication fails
     */
    @CircuitBreaker(name = "cartService", fallbackMethod = "getCartFallback")
    public CartResponse getCart(UUID cartId) {
        try {
            log.debug("Fetching cart: {}", cartId);

            String url = properties.baseUrl() + "/api/v1/carts/internal/" + cartId;

            ResponseEntity<CartResponse> response = restTemplate.getForEntity(
                    url,
                    CartResponse.class
            );

            if (response.getBody() == null) {
                throw new CartServiceException("Empty response from cart service");
            }

            log.debug("Successfully fetched cart: {}", cartId);
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error calling cart service: {} - {}", e.getStatusCode(), e.getMessage());
            throw new CartServiceException("Failed to fetch cart: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            log.error("Connection error calling cart service: {}", e.getMessage());
            throw new CartServiceException("Cart service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling cart service: {}", e.getMessage(), e);
            throw new CartServiceException("Unexpected error fetching cart", e);
        }
    }

    /**
     * Fallback method when cart service is unavailable.
     */
    private CartResponse getCartFallback(UUID cartId, Exception e) {
        log.error("Cart service circuit breaker activated for cart: {}", cartId, e);
        throw new CartServiceException("Cart service is currently unavailable. Please try again later.");
    }

    /**
     * Cart response DTO.
     * This should ideally be in shared-dtos, but defined here for now.
     */
    public record CartResponse(
            UUID cartId,
            UUID userId,
            String status,
            @JsonProperty("subtotal")
            BigDecimal subtotalAmount,
            List<CartItemResponse> items
    ) {
    }

    /**
     * Cart item response DTO.
     */
    public record CartItemResponse(
            UUID cartItemId,
            UUID productId,
            String sku,
            String titleSnapshot,
            BigDecimal unitPriceSnapshot,
            Integer quantity,
            @JsonProperty("subtotal")
            BigDecimal lineTotalAmount
    ) {
    }
}
