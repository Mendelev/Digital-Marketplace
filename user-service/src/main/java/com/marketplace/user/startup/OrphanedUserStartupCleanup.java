package com.marketplace.user.startup;

import com.marketplace.user.domain.model.User;
import com.marketplace.user.domain.repository.UserRepository;
import com.marketplace.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Startup task to cleanup orphaned users that don't have credentials in Auth Service.
 * This handles cases where User Service succeeded but Auth Service failed before
 * the cleanup scheduler could run.
 */
@Component
public class OrphanedUserStartupCleanup {

    private static final Logger log = LoggerFactory.getLogger(OrphanedUserStartupCleanup.class);

    private final UserRepository userRepository;
    private final UserService userService;
    // TODO: Create AuthServiceClient to query Auth Service for valid userIds

    public OrphanedUserStartupCleanup(
            UserRepository userRepository,
            UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Run cleanup after application is fully started.
     * This ensures database connections and all beans are initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOrphanedUsersOnStartup() {
        log.info("Starting orphaned user cleanup on application startup");

        try {
            List<User> allUsers = userRepository.findAll();
            log.info("Found {} total users to validate", allUsers.size());

            int orphanedCount = 0;

            for (User user : allUsers) {
                if (isOrphanedUser(user.getUserId())) {
                    log.warn("Found orphaned user without credentials: userId={}, email={}", 
                            user.getUserId(), user.getEmail());
                    
                    // Delete orphaned user
                    userService.deleteUser(user.getUserId());
                    orphanedCount++;
                    
                    log.info("Deleted orphaned user: userId={}", user.getUserId());
                }
            }

            if (orphanedCount > 0) {
                log.info("Startup cleanup completed: deleted {} orphaned users", orphanedCount);
            } else {
                log.info("Startup cleanup completed: no orphaned users found");
            }

        } catch (Exception e) {
            log.error("Error during startup cleanup of orphaned users", e);
            // Don't fail application startup if cleanup fails
        }
    }

    /**
     * Check if user is orphaned (exists in User Service but not in Auth Service).
     * TODO: Implement by querying Auth Service /api/v1/auth/validate-user/{userId} endpoint.
     * For now, this is a placeholder that always returns false.
     */
    private boolean isOrphanedUser(UUID userId) {
        // TODO: Call Auth Service to check if credentials exist
        // For now, disable this check until Auth Service endpoint is implemented
        return false;
    }
}
