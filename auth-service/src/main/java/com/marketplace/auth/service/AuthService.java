package com.marketplace.auth.service;

import com.marketplace.auth.client.UserServiceClient;
import com.marketplace.auth.config.JwtProperties;
import com.marketplace.auth.domain.model.Credential;
import com.marketplace.auth.domain.model.OrphanedUser;
import com.marketplace.auth.domain.model.PasswordResetToken;
import com.marketplace.auth.domain.model.RefreshToken;
import com.marketplace.auth.domain.repository.CredentialRepository;
import com.marketplace.auth.domain.repository.OrphanedUserRepository;
import com.marketplace.auth.domain.repository.PasswordResetTokenRepository;
import com.marketplace.auth.domain.repository.RefreshTokenRepository;
import com.marketplace.auth.dto.auth.*;
import com.marketplace.auth.metrics.RegistrationMetrics;
import com.marketplace.shared.dto.CreateUserRequest;
import com.marketplace.shared.dto.UserResponse;
import com.marketplace.auth.exception.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Core authentication service containing business logic for user authentication,
 * token management, and password recovery.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_ROLE = "CUSTOMER";
    private static final int RESET_TOKEN_BYTES = 32;
    private static final int RESET_TOKEN_EXPIRATION_HOURS = 1;
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private final CredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OrphanedUserRepository orphanedUserRepository;
    private final UserServiceClient userServiceClient;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final RegistrationMetrics registrationMetrics;
    private final SecureRandom secureRandom;

    public AuthService(
            CredentialRepository credentialRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            OrphanedUserRepository orphanedUserRepository,
            UserServiceClient userServiceClient,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            JwtProperties jwtProperties,
            RegistrationMetrics registrationMetrics) {
        this.credentialRepository = credentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.orphanedUserRepository = orphanedUserRepository;
        this.userServiceClient = userServiceClient;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.registrationMetrics = registrationMetrics;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Register a new user with credential creation and User Service integration.
     * Implements orchestration pattern: creates user in User Service FIRST,
     * then saves credentials. If credential save fails, performs compensating
     * transaction to delete user from User Service.
     *
     * @param request registration request
     * @return authentication response with tokens
     */
    public AuthResponse register(RegisterRequest request) {
        Timer.Sample timerSample = registrationMetrics.startRegistrationTimer();
        MDC.put("operation", "register");
        log.info("Processing registration for email: {}", request.email());

        try {
            // Check if email already exists
            if (credentialRepository.existsByEmail(request.email())) {
                log.warn("Registration failed: email already exists - {}", request.email());
                registrationMetrics.incrementRegistrationFailure();
                throw new DuplicateEmailException(request.email());
            }

            UUID userId = UUID.randomUUID();
            MDC.put("userId", userId.toString());

            // Step 1: Create user in User Service FIRST (outside transaction boundary)
            CreateUserRequest createUserRequest = new CreateUserRequest(
                    userId,
                    request.email(),
                    request.name(),
                    List.of(DEFAULT_ROLE)
            );
            
            UserResponse userResponse = userServiceClient.createUser(createUserRequest);
            log.info("User created successfully in User Service: {}", userId);

            // Step 2: Save credentials in Auth Service (within transaction)
            try {
                AuthResponse response = saveCredentialsAndGenerateTokens(userId, request);
                registrationMetrics.incrementRegistrationSuccess();
                registrationMetrics.recordRegistrationTime(timerSample);
                return response;
            } catch (Exception e) {
                log.error("Failed to save credentials for user: {}. Initiating compensating transaction.", userId, e);
                registrationMetrics.incrementRegistrationFailure();
                
                // Step 3: Compensating transaction - delete user from User Service
                try {
                    userServiceClient.deleteUser(userId);
                    log.info("Successfully deleted user from User Service as part of compensating transaction: {}", userId);
                    registrationMetrics.incrementCompensatingDeleteSuccess();
                } catch (Exception deleteException) {
                    // If compensating delete fails, save to orphaned_users table for cleanup
                    log.error("Failed to delete user from User Service. Saving to orphaned_users table: {}", 
                            userId, deleteException);
                    registrationMetrics.incrementCompensatingDeleteFailure();
                    saveOrphanedUser(userId, request.email());
                }
                
                // Re-throw original exception to return error to client
                throw e;
            }
        } catch (Exception e) {
            registrationMetrics.recordRegistrationTime(timerSample);
            throw e;
        }
    }

    /**
     * Save credentials and generate tokens within a transaction.
     * This method is separate to ensure transaction boundary is clear.
     */
    @Transactional
    private AuthResponse saveCredentialsAndGenerateTokens(UUID userId, RegisterRequest request) {
        // Create and save credential
        String passwordHash = passwordEncoder.encode(request.password());
        Credential credential = new Credential(userId, request.email(), passwordHash);
        credential = credentialRepository.save(credential);
        
        log.debug("Credential created for user: {}", userId);

        // Generate tokens
        List<String> roles = List.of(DEFAULT_ROLE);
        String accessToken = jwtService.generateAccessToken(userId, request.email(), roles);
        String refreshToken = jwtService.generateRefreshToken(userId);

        // Save hashed refresh token
        saveRefreshToken(userId, refreshToken);

        log.info("Registration completed successfully for user: {}", userId);
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenExpirationMinutes() * 60,
                userId,
                request.email(),
                roles
        );
    }

    /**
     * Save orphaned user record for cleanup scheduler.
     */
    @Transactional
    private void saveOrphanedUser(UUID userId, String email) {
        try {
            OrphanedUser orphanedUser = new OrphanedUser(userId, email);
            orphanedUserRepository.save(orphanedUser);
            registrationMetrics.incrementOrphanedUserSaved();
            log.info("Saved orphaned user record for cleanup: userId={}, email={}", userId, email);
        } catch (Exception e) {
            log.error("Failed to save orphaned user record: userId={}, email={}", userId, email, e);
            // Don't throw - this is best effort logging
        }
    }

    /**
     * Authenticate user and issue tokens.
     *
     * @param request login request
     * @return authentication response with tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        MDC.put("operation", "login");
        log.info("Processing login for email: {}", request.email());

        // Find credential
        Credential credential = credentialRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed: credential not found for email - {}", request.email());
                    return new InvalidCredentialsException();
                });

        MDC.put("userId", credential.getUserId().toString());

        // Check if account is locked
        if (credential.isLocked()) {
            log.warn("Login failed: account is locked - {}", request.email());
            throw new AccountLockedException();
        }

        // Validate password
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            log.warn("Login failed: invalid password for email - {}", request.email());
            credential.incrementFailedLoginCount();
            credentialRepository.save(credential);
            
            if (credential.isLocked()) {
                log.warn("Account locked after {} failed attempts - {}", 
                        MAX_FAILED_LOGIN_ATTEMPTS, request.email());
                throw new AccountLockedException();
            }
            
            throw new InvalidCredentialsException();
        }

        // Reset failed login count on successful login
        if (credential.getFailedLoginCount() > 0) {
            credential.resetFailedLoginCount();
            credentialRepository.save(credential);
            log.debug("Failed login count reset for user: {}", credential.getUserId());
        }

        // Fetch user roles from User Service
        List<String> roles = fetchUserRoles(credential.getUserId());
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                credential.getUserId(), 
                credential.getEmail(), 
                roles
        );
        String refreshToken = jwtService.generateRefreshToken(credential.getUserId());

        // Save hashed refresh token
        saveRefreshToken(credential.getUserId(), refreshToken);

        log.info("Login successful for user: {} with roles: {}", credential.getUserId(), roles);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtProperties.accessTokenExpirationMinutes() * 60,
                credential.getUserId(),
                credential.getEmail(),
                roles
        );
    }

    /**
     * Fetch user roles from User Service.
     * Falls back to default role if User Service is unavailable.
     */
    private List<String> fetchUserRoles(UUID userId) {
        try {
            UserResponse userResponse = userServiceClient.getUserById(userId);
            if (userResponse != null && userResponse.roles() != null && !userResponse.roles().isEmpty()) {
                log.debug("Fetched roles from User Service for user {}: {}", userId, userResponse.roles());
                return userResponse.roles();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch roles from User Service for user {}: {}. Using default role.", 
                    userId, e.getMessage());
        }
        return List.of(DEFAULT_ROLE);
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token request
     * @return new authentication response with tokens
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        MDC.put("operation", "refreshToken");
        log.info("Processing token refresh");

        // Hash the incoming refresh token to look up in database
        String tokenHash = passwordEncoder.encode(request.refreshToken());

        // Find refresh token by comparing hashes
        RefreshToken storedToken = refreshTokenRepository.findAll().stream()
                .filter(rt -> passwordEncoder.matches(request.refreshToken(), rt.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Refresh failed: token not found");
                    return new InvalidTokenException("Invalid refresh token");
                });

        MDC.put("userId", storedToken.getUserId().toString());

        // Validate token
        if (storedToken.isRevoked()) {
            log.warn("Refresh failed: token has been revoked");
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (storedToken.isExpired()) {
            log.warn("Refresh failed: token has expired");
            throw new TokenExpiredException();
        }

        // Get credential for user details
        Credential credential = credentialRepository.findByUserId(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User credential not found"));

        // Generate new tokens
        List<String> roles = List.of(DEFAULT_ROLE);
        String newAccessToken = jwtService.generateAccessToken(
                credential.getUserId(),
                credential.getEmail(),
                roles
        );
        String newRefreshToken = jwtService.generateRefreshToken(credential.getUserId());

        // Revoke old refresh token
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        // Save new refresh token
        saveRefreshToken(credential.getUserId(), newRefreshToken);

        log.info("Token refresh successful for user: {}", credential.getUserId());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                jwtProperties.accessTokenExpirationMinutes() * 60,
                credential.getUserId(),
                credential.getEmail(),
                roles
        );
    }

    /**
     * Initiate password reset by generating and storing reset token.
     *
     * @param request forgot password request
     * @return success message
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        MDC.put("operation", "forgotPassword");
        log.info("Processing password reset request for email: {}", request.email());

        // Find credential (silently fail if not found for security)
        credentialRepository.findByEmail(request.email()).ifPresent(credential -> {
            MDC.put("userId", credential.getUserId().toString());

            // Generate secure random token
            byte[] tokenBytes = new byte[RESET_TOKEN_BYTES];
            secureRandom.nextBytes(tokenBytes);
            String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

            // Hash token for storage
            String tokenHash = passwordEncoder.encode(resetToken);

            // Create reset token entity
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRATION_HOURS);
            PasswordResetToken passwordResetToken = new PasswordResetToken(
                    request.email(),
                    tokenHash,
                    expiresAt
            );
            passwordResetTokenRepository.save(passwordResetToken);

            log.info("Password reset token created for user: {}", credential.getUserId());
            // In production, send email with resetToken here
            log.debug("Reset token (would be sent via email): {}", resetToken);
        });

        // Always return success message for security (don't reveal if email exists)
        return new MessageResponse(
                "If the email address exists, a password reset link has been sent."
        );
    }

    /**
     * Reset password using reset token.
     *
     * @param request reset password request
     * @return success message
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        MDC.put("operation", "resetPassword");
        log.info("Processing password reset with token");

        // Find reset token by comparing hashes
        PasswordResetToken resetToken = passwordResetTokenRepository.findAll().stream()
                .filter(prt -> passwordEncoder.matches(request.token(), prt.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Password reset failed: invalid token");
                    return new InvalidTokenException("Invalid or expired reset token");
                });

        // Validate token
        if (resetToken.isUsed()) {
            log.warn("Password reset failed: token already used");
            throw new InvalidTokenException("Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            log.warn("Password reset failed: token expired");
            throw new TokenExpiredException();
        }

        // Find credential
        Credential credential = credentialRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User credential not found"));

        MDC.put("userId", credential.getUserId().toString());

        // Update password
        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        credential.setPasswordHash(newPasswordHash);
        credential.activate(); // Reset failed login count and unlock if locked
        credentialRepository.save(credential);

        // Mark token as used
        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", credential.getUserId());

        return new MessageResponse("Password has been reset successfully");
    }

    /**
     * Validate JWT token and return user information.
     *
     * @param request validation request
     * @return token validation response
     */
    public TokenValidationResponse validateToken(ValidateTokenRequest request) {
        MDC.put("operation", "validateToken");
        log.debug("Validating token");

        try {
            Claims claims = jwtService.validateAndParseToken(request.token());
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            Instant expiresAt = claims.getExpiration().toInstant();

            MDC.put("userId", userId.toString());
            log.debug("Token validated successfully for user: {}", userId);

            return new TokenValidationResponse(true, userId, email, roles, expiresAt);

        } catch (ExpiredJwtException e) {
            log.warn("Token validation failed: token expired");
            throw new TokenExpiredException();
        } catch (JwtException e) {
            log.warn("Token validation failed: invalid token - {}", e.getMessage());
            throw new InvalidTokenException("Invalid token", e);
        }
    }

    /**
     * Clean up old refresh tokens.
     *
     * @param request cleanup request with parameters
     * @return cleanup response with count of deleted tokens
     */
    @Transactional
    public CleanupTokensResponse cleanupTokens(CleanupTokensRequest request) {
        MDC.put("operation", "cleanupTokens");
        log.info("Cleaning up tokens older than {} days, includeActive={}", 
                request.olderThanDays(), request.includeActive());

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(request.olderThanDays());
        int deletedCount;

        if (request.includeActive()) {
            deletedCount = refreshTokenRepository.deleteAllOlderThan(cutoffDate);
        } else {
            deletedCount = refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now(), cutoffDate);
        }

        log.info("Token cleanup completed: {} tokens deleted", deletedCount);

        return new CleanupTokensResponse(
                deletedCount,
                String.format("Successfully deleted %d refresh tokens", deletedCount)
        );
    }

    /**
     * Save hashed refresh token to database.
     */
    private void saveRefreshToken(UUID userId, String refreshToken) {
        String tokenHash = passwordEncoder.encode(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(jwtProperties.refreshTokenExpirationDays());
        
        RefreshToken token = new RefreshToken(userId, tokenHash, expiresAt);
        refreshTokenRepository.save(token);
        
        log.debug("Refresh token saved for user: {}", userId);
    }
}
