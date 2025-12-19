package com.marketplace.order.client;

import com.marketplace.order.config.UserServiceProperties;
import com.marketplace.order.exception.UserServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Client for communicating with User Service.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;
    private final UserServiceProperties properties;

    public UserServiceClient(RestTemplate restTemplate, UserServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Get address by ID.
     *
     * @param addressId Address ID
     * @return AddressResponse
     * @throws UserServiceException if communication fails
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "getAddressFallback")
    public AddressResponse getAddress(UUID addressId) {
        try {
            log.debug("Fetching address: {}", addressId);

            String url = properties.baseUrl() + "/api/v1/addresses/" + addressId;

            ResponseEntity<AddressResponse> response = restTemplate.getForEntity(
                    url,
                    AddressResponse.class
            );

            if (response.getBody() == null) {
                throw new UserServiceException("Empty response from user service");
            }

            log.debug("Successfully fetched address: {}", addressId);
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error calling user service: {} - {}", e.getStatusCode(), e.getMessage());
            throw new UserServiceException("Failed to fetch address: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            log.error("Connection error calling user service: {}", e.getMessage());
            throw new UserServiceException("User service unavailable: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling user service: {}", e.getMessage(), e);
            throw new UserServiceException("Unexpected error fetching address", e);
        }
    }

    /**
     * Fallback method when user service is unavailable.
     */
    private AddressResponse getAddressFallback(UUID addressId, Exception e) {
        log.error("User service circuit breaker activated for address: {}", addressId, e);
        throw new UserServiceException("User service is currently unavailable. Please try again later.");
    }

    /**
     * Address response DTO.
     * This should ideally be in shared-dtos, but defined here for now.
     */
    public record AddressResponse(
            UUID addressId,
            String label,
            String country,
            String state,
            String city,
            String zip,
            String street,
            String number,
            String complement
    ) {
    }
}
