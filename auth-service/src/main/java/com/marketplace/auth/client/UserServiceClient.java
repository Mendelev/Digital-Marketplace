package com.marketplace.auth.client;

import com.marketplace.auth.config.UserServiceProperties;
import com.marketplace.shared.dto.CreateUserRequest;
import com.marketplace.shared.dto.UserResponse;
import com.marketplace.auth.exception.UserServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Client for interacting with User Service.
 * Includes circuit breaker for resilience.
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    private static final String CREATE_USER_ENDPOINT = "/api/v1/users";
    private static final String INTERNAL_USER_ENDPOINT = "/api/v1/users/internal";
    private static final String SERVICE_SECRET_HEADER = "X-Service-Secret";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final RestTemplate restTemplate;
    private final UserServiceProperties properties;

    public UserServiceClient(RestTemplate restTemplate, UserServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Create a new user in the User Service.
     *
     * @param request the user creation request
     * @return the created user response
     * @throws UserServiceException if user creation fails
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "createUserFallback")
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user in User Service: email={}, userId={}", request.email(), request.userId());
        
        try {
            String url = properties.baseUrl() + CREATE_USER_ENDPOINT;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(SERVICE_SECRET_HEADER, properties.sharedSecret());
            
            // Propagate correlation ID for distributed tracing
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                headers.set(CORRELATION_ID_HEADER, correlationId);
            }
            
            HttpEntity<CreateUserRequest> httpEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                    url,
                    httpEntity,
                    UserResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("User created successfully in User Service: userId={}", response.getBody().userId());
                return response.getBody();
            } else {
                log.error("Unexpected response from User Service: status={}", response.getStatusCode());
                throw new UserServiceException("Unexpected response from User Service: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            log.error("Client error from User Service: status={}, message={}", 
                    e.getStatusCode(), e.getMessage());
            throw new UserServiceException("User Service returned client error: " + e.getStatusCode(), e);
            
        } catch (HttpServerErrorException e) {
            log.error("Server error from User Service: status={}, message={}", 
                    e.getStatusCode(), e.getMessage());
            throw new UserServiceException("User Service returned server error: " + e.getStatusCode(), e);
            
        } catch (ResourceAccessException e) {
            log.error("Failed to connect to User Service: {}", e.getMessage());
            throw new UserServiceException("Failed to connect to User Service", e);
        }
    }

    /**
     * Delete a user from the User Service (compensating transaction).
     * This is an internal operation used for cleanup when Auth Service fails.
     *
     * @param userId the user ID to delete
     * @throws UserServiceException if user deletion fails
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "deleteUserFallback")
    public void deleteUser(UUID userId) {
        log.info("Deleting user from User Service: userId={}", userId);
        
        try {
            String url = properties.baseUrl() + CREATE_USER_ENDPOINT + "/" + userId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set(SERVICE_SECRET_HEADER, properties.sharedSecret());
            
            // Propagate correlation ID for distributed tracing
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                headers.set(CORRELATION_ID_HEADER, correlationId);
            }
            
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            
            restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.DELETE,
                    httpEntity,
                    Void.class
            );
            
            log.info("User deleted successfully from User Service: userId={}", userId);
            
        } catch (HttpClientErrorException e) {
            log.error("Client error deleting user from User Service: status={}, userId={}", 
                    e.getStatusCode(), userId);
            throw new UserServiceException("User Service returned client error during deletion: " + e.getStatusCode(), e);
            
        } catch (HttpServerErrorException e) {
            log.error("Server error deleting user from User Service: status={}, userId={}", 
                    e.getStatusCode(), userId);
            throw new UserServiceException("User Service returned server error during deletion: " + e.getStatusCode(), e);
            
        } catch (ResourceAccessException e) {
            log.error("Failed to connect to User Service for deletion: userId={}", userId);
            throw new UserServiceException("Failed to connect to User Service for deletion", e);
        }
    }

    /**
     * Get user by ID from the User Service.
     * Used to fetch user roles during login.
     *
     * @param userId the user ID to fetch
     * @return the user response with roles
     * @throws UserServiceException if user fetch fails
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    public UserResponse getUserById(UUID userId) {
        log.debug("Fetching user from User Service: userId={}", userId);
        
        try {
            String url = properties.baseUrl() + INTERNAL_USER_ENDPOINT + "/" + userId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set(SERVICE_SECRET_HEADER, properties.sharedSecret());
            
            // Propagate correlation ID for distributed tracing
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                headers.set(CORRELATION_ID_HEADER, correlationId);
            }
            
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
            
            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    httpEntity,
                    UserResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("User fetched successfully from User Service: userId={}, roles={}", 
                        userId, response.getBody().roles());
                return response.getBody();
            } else {
                log.error("Unexpected response from User Service: status={}", response.getStatusCode());
                throw new UserServiceException("Unexpected response from User Service: " + response.getStatusCode());
            }
            
        } catch (HttpClientErrorException e) {
            log.error("Client error fetching user from User Service: status={}, userId={}", 
                    e.getStatusCode(), userId);
            throw new UserServiceException("User Service returned client error: " + e.getStatusCode(), e);
            
        } catch (HttpServerErrorException e) {
            log.error("Server error fetching user from User Service: status={}, userId={}", 
                    e.getStatusCode(), userId);
            throw new UserServiceException("User Service returned server error: " + e.getStatusCode(), e);
            
        } catch (ResourceAccessException e) {
            log.error("Failed to connect to User Service: userId={}", userId);
            throw new UserServiceException("Failed to connect to User Service", e);
        }
    }

    /**
     * Fallback method when User Service is unavailable.
     * This method is called when circuit breaker is open.
     */
    @SuppressWarnings("unused")
    private UserResponse createUserFallback(CreateUserRequest request, Exception e) {
        log.error("Circuit breaker activated for User Service. Fallback triggered for user: {}", 
                request.email(), e);
        throw new UserServiceException("User Service is currently unavailable. Please try again later.", e);
    }

    /**
     * Fallback method for deleteUser when User Service is unavailable.
     */
    @SuppressWarnings("unused")
    private void deleteUserFallback(UUID userId, Exception e) {
        log.error("Circuit breaker activated for User Service. Delete fallback triggered for user: {}", 
                userId, e);
        throw new UserServiceException("User Service is currently unavailable for deletion.", e);
    }

    /**
     * Fallback method for getUserById when User Service is unavailable.
     * Returns null to allow login to proceed with default role.
     */
    @SuppressWarnings("unused")
    private UserResponse getUserByIdFallback(UUID userId, Exception e) {
        log.warn("Circuit breaker activated for User Service. GetUserById fallback triggered for user: {}. Using default role.", 
                userId);
        return null; // Returns null so login can proceed with default role
    }
}
