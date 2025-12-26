package com.marketplace.auth.service;

import com.marketplace.auth.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for JWT token generation, validation, and management.
 * Uses RS256 algorithm with RSA key pair.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ROLES_CLAIM = "roles";
    private static final String EMAIL_CLAIM = "email";

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;
    
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public JwtService(JwtProperties jwtProperties, ResourceLoader resourceLoader) {
        this.jwtProperties = jwtProperties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        log.info("Initializing JWT service with RS256 algorithm");
        this.privateKey = loadPrivateKey(jwtProperties.privateKeyPath());
        this.publicKey = loadPublicKey(jwtProperties.publicKeyPath());
        log.info("JWT keys loaded successfully");
    }

    /**
     * Generate an access token for the user.
     *
     * @param userId the user ID
     * @param email the user email
     * @param roles the user roles
     * @return JWT access token string
     */
    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.accessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(EMAIL_CLAIM, email)
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generate a refresh token for the user.
     *
     * @param userId the user ID
     * @return JWT refresh token string
     */
    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.refreshTokenExpirationDays(), ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Validate and parse a JWT token.
     *
     * @param token the JWT token string
     * @return Claims object containing token data
     * @throws JwtException if token is invalid or expired
     */
    public Claims validateAndParseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract user ID from token.
     *
     * @param token the JWT token string
     * @return user ID as UUID
     */
    public UUID extractUserId(String token) {
        Claims claims = validateAndParseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract email from token.
     *
     * @param token the JWT token string
     * @return email address
     */
    public String extractEmail(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.get(EMAIL_CLAIM, String.class);
    }

    /**
     * Extract roles from token.
     *
     * @param token the JWT token string
     * @return list of role strings
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.get(ROLES_CLAIM, List.class);
    }

    /**
     * Check if token is expired.
     *
     * @param token the JWT token string
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndParseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Get the public key in PEM format for distribution to other services.
     *
     * @return PEM-encoded public key string
     */
    public String getPublicKeyPem() throws IOException {
        Resource resource = resourceLoader.getResource(jwtProperties.publicKeyPath());
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Load private key from PEM file.
     */
    private PrivateKey loadPrivateKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Resource resource = resourceLoader.getResource(path);
        String keyContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        
        // Remove PEM headers and whitespace
        keyContent = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Load public key from PEM file.
     */
    private PublicKey loadPublicKey(String path) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Resource resource = resourceLoader.getResource(path);
        String keyContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        
        // Remove PEM headers and whitespace
        keyContent = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}
