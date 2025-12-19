package com.marketplace.payment.security;

import com.marketplace.payment.client.AuthServiceClient;
import com.marketplace.payment.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

/**
 * Service for JWT validation.
 */
@Service
public class JwtValidationService {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationService.class);

    private final AuthServiceClient authServiceClient;

    public JwtValidationService(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    /**
     * Validate JWT token and return claims.
     * Implements reactive public key fetching on validation failure.
     */
    public Claims validateToken(String token) {
        try {
            PublicKey publicKey = authServiceClient.getPublicKey();
            return parseToken(token, publicKey);

        } catch (ExpiredJwtException e) {
            log.warn("Token validation failed: token expired");
            throw new InvalidTokenException("Token has expired");

        } catch (JwtException e) {
            // On validation failure, try fetching fresh public key
            log.warn("Token validation failed, attempting with fresh public key: {}", e.getMessage());

            try {
                authServiceClient.clearCache();
                PublicKey freshPublicKey = authServiceClient.getPublicKey();
                return parseToken(token, freshPublicKey);

            } catch (Exception retryException) {
                log.error("Token validation failed even with fresh public key", retryException);
                throw new InvalidTokenException("Invalid token", retryException);
            }
        }
    }

    /**
     * Parse token with given public key.
     */
    private Claims parseToken(String token, PublicKey publicKey) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract user ID from claims.
     */
    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract email from claims.
     */
    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    /**
     * Extract roles from claims.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return claims.get("roles", List.class);
    }
}
