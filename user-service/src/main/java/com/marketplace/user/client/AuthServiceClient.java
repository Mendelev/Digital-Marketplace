package com.marketplace.user.client;

import com.marketplace.user.config.AuthServiceProperties;
import com.marketplace.user.exception.AuthServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Client for fetching JWT public key from Auth Service.
 * Implements reactive caching with circuit breaker.
 */
@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestTemplate restTemplate;
    private final AuthServiceProperties properties;

    // Reactive cache
    private PublicKey cachedPublicKey;
    private LocalDateTime cacheExpiration;

    public AuthServiceClient(RestTemplate restTemplate, AuthServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * Get public key with reactive caching.
     * Fetches from Auth Service on cache miss or expiration.
     */
    public synchronized PublicKey getPublicKey() {
        if (cachedPublicKey != null && LocalDateTime.now().isBefore(cacheExpiration)) {
            log.debug("Using cached public key");
            return cachedPublicKey;
        }

        log.info("Public key cache miss or expired, fetching from Auth Service");
        return fetchPublicKeyFromAuthService();
    }

    /**
     * Fetch public key from Auth Service with circuit breaker.
     */
    @CircuitBreaker(name = "authService", fallbackMethod = "getCachedPublicKeyFallback")
    private PublicKey fetchPublicKeyFromAuthService() {
        try {
            String url = properties.baseUrl() + properties.publicKeyEndpoint();
            log.debug("Fetching public key from: {}", url);

            String publicKeyPem = restTemplate.getForObject(url, String.class);
            
            if (publicKeyPem == null || publicKeyPem.isEmpty()) {
                throw new AuthServiceException("Public key response is empty");
            }

            PublicKey publicKey = parsePublicKey(publicKeyPem);
            
            // Update cache
            cachedPublicKey = publicKey;
            cacheExpiration = LocalDateTime.now().plusMinutes(properties.publicKeyCacheTtlMinutes());
            
            log.info("Public key fetched and cached successfully");
            return publicKey;

        } catch (Exception e) {
            log.error("Failed to fetch public key from Auth Service", e);
            throw new AuthServiceException("Failed to fetch public key", e);
        }
    }

    /**
     * Fallback method for circuit breaker.
     * Returns cached key even if expired as last resort.
     */
    @SuppressWarnings("unused")
    private PublicKey getCachedPublicKeyFallback(Exception e) {
        log.warn("Circuit breaker activated, attempting to use cached public key", e);
        
        if (cachedPublicKey != null) {
            log.info("Using expired cached public key as fallback");
            return cachedPublicKey;
        }
        
        log.error("No cached public key available for fallback");
        throw new AuthServiceException("Auth Service unavailable and no cached key available", e);
    }

    /**
     * Parse PEM-encoded public key string to PublicKey object.
     */
    private PublicKey parsePublicKey(String publicKeyPem) {
        try {
            String publicKeyContent = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            
            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            log.error("Failed to parse public key", e);
            throw new AuthServiceException("Invalid public key format", e);
        }
    }

    /**
     * Clear cached public key (useful for testing or manual refresh).
     */
    public synchronized void clearCache() {
        log.info("Clearing public key cache");
        cachedPublicKey = null;
        cacheExpiration = null;
    }
}
