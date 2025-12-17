package com.marketplace.auth.scheduler;

import com.marketplace.auth.client.UserServiceClient;
import com.marketplace.auth.domain.model.OrphanedUser;
import com.marketplace.auth.domain.repository.OrphanedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled task to cleanup orphaned users from User Service.
 * Runs every 5 minutes to retry failed compensating transactions.
 */
@Component
public class OrphanedUserCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrphanedUserCleanupScheduler.class);
    private static final int MAX_RETRY_COUNT = 10;

    private final OrphanedUserRepository orphanedUserRepository;
    private final UserServiceClient userServiceClient;

    public OrphanedUserCleanupScheduler(
            OrphanedUserRepository orphanedUserRepository,
            UserServiceClient userServiceClient) {
        this.orphanedUserRepository = orphanedUserRepository;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Cleanup orphaned users with PENDING status.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void cleanupOrphanedUsers() {
        List<OrphanedUser> pendingUsers = orphanedUserRepository.findByStatus(
                OrphanedUser.OrphanedUserStatus.PENDING);

        if (pendingUsers.isEmpty()) {
            log.debug("No orphaned users to cleanup");
            return;
        }

        log.info("Starting orphaned user cleanup. Found {} pending users", pendingUsers.size());

        for (OrphanedUser orphanedUser : pendingUsers) {
            processOrphanedUser(orphanedUser);
        }

        log.info("Orphaned user cleanup completed");
    }

    /**
     * Process a single orphaned user.
     */
    private void processOrphanedUser(OrphanedUser orphanedUser) {
        try {
            log.info("Attempting to delete orphaned user: userId={}, retryCount={}", 
                    orphanedUser.getUserId(), orphanedUser.getRetryCount());

            // Attempt to delete user from User Service
            userServiceClient.deleteUser(orphanedUser.getUserId());

            // Mark as completed
            orphanedUser.markCompleted();
            orphanedUserRepository.save(orphanedUser);

            log.info("Successfully deleted orphaned user: userId={}", orphanedUser.getUserId());

        } catch (Exception e) {
            log.error("Failed to delete orphaned user: userId={}, retryCount={}", 
                    orphanedUser.getUserId(), orphanedUser.getRetryCount(), e);

            // Increment retry count
            orphanedUser.incrementRetryCount();

            // Mark as failed if max retries exceeded
            if (orphanedUser.getRetryCount() >= MAX_RETRY_COUNT) {
                orphanedUser.markFailed();
                log.error("Max retries exceeded for orphaned user: userId={}, marking as FAILED", 
                        orphanedUser.getUserId());
            }

            orphanedUserRepository.save(orphanedUser);
        }
    }
}
