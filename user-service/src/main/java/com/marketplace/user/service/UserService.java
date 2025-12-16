package com.marketplace.user.service;

import com.marketplace.user.domain.model.User;
import com.marketplace.user.domain.model.UserPreferences;
import com.marketplace.user.domain.repository.UserPreferencesRepository;
import com.marketplace.user.domain.repository.UserRepository;
import com.marketplace.user.dto.user.CreateUserRequest;
import com.marketplace.user.dto.user.UpdateUserRequest;
import com.marketplace.user.dto.user.UserResponse;
import com.marketplace.user.exception.DuplicateEmailException;
import com.marketplace.user.exception.ResourceNotFoundException;
import com.marketplace.user.exception.UnauthorizedException;
import com.marketplace.user.security.AuthenticatedUser;
import com.marketplace.user.util.EventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for user management.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final EventLogger eventLogger;

    public UserService(UserRepository userRepository,
                      UserPreferencesRepository preferencesRepository,
                      EventLogger eventLogger) {
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.eventLogger = eventLogger;
    }

    /**
     * Create a new user (called by Auth Service during registration).
     * Automatically creates default preferences.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        MDC.put("operation", "createUser");
        MDC.put("userId", request.userId().toString());

        log.info("Creating user: {}", request.email());

        // Check for duplicate email
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Email already exists: " + request.email());
        }

        // Create user
        User user = new User(request.userId(), request.email(), request.name(), new HashSet<>(request.roles()));
        user = userRepository.save(user);

        // Create default preferences
        UserPreferences preferences = new UserPreferences(user.getUserId());
        preferencesRepository.save(preferences);

        log.info("User created successfully: {}", user.getUserId());

        // Log event
        eventLogger.logEvent("UserCreated", "USER", user.getUserId(), toResponse(user));

        return toResponse(user);
    }

    /**
     * Get user by ID.
     * Users can only access their own profile unless they are ADMIN.
     */
    public UserResponse getUserById(UUID userId, AuthenticatedUser authenticatedUser) {
        validateUserAccess(userId, authenticatedUser);

        MDC.put("operation", "getUserById");
        MDC.put("userId", userId.toString());

        log.debug("Fetching user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return toResponse(user);
    }

    /**
     * Update user profile.
     * Users can only update their own profile unless they are ADMIN.
     */
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request, AuthenticatedUser authenticatedUser) {
        validateUserAccess(userId, authenticatedUser);

        MDC.put("operation", "updateUser");
        MDC.put("userId", userId.toString());

        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Track changes
        Map<String, Object> changedFields = new HashMap<>();

        if (request.name() != null && !request.name().equals(user.getName())) {
            user.setName(request.name());
            changedFields.put("name", request.name());
        }

        if (request.phone() != null && !request.phone().equals(user.getPhone())) {
            user.setPhone(request.phone());
            changedFields.put("phone", request.phone());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            // Check for duplicate email
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateEmailException("Email already exists: " + request.email());
            }
            user.setEmail(request.email());
            changedFields.put("email", request.email());
        }

        if (changedFields.isEmpty()) {
            log.debug("No changes detected for user: {}", userId);
            return toResponse(user);
        }

        user = userRepository.save(user);

        log.info("User updated successfully: {}, changes: {}", userId, changedFields.keySet());

        // Log event with changed fields only
        eventLogger.logEvent("UserUpdated", "USER", user.getUserId(), changedFields);

        return toResponse(user);
    }

    /**
     * Validate that the authenticated user can access the resource.
     * Access is granted if:
     * - User is accessing their own resource
     * - User has ADMIN role
     */
    private void validateUserAccess(UUID resourceUserId, AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        boolean isOwner = resourceUserId.equals(authenticatedUser.getUserId());
        boolean isAdmin = authenticatedUser.isAdmin();

        if (!isOwner && !isAdmin) {
            log.warn("Unauthorized access attempt by user {} to resource of user {}",
                    authenticatedUser.getUserId(), resourceUserId);
            throw new UnauthorizedException("Access denied");
        }
    }

    /**
     * Convert User entity to UserResponse DTO.
     */
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                new ArrayList<>(user.getRoles()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
