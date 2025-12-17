package com.marketplace.auth.domain.repository;

import com.marketplace.auth.domain.model.OrphanedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for OrphanedUser entity.
 */
@Repository
public interface OrphanedUserRepository extends JpaRepository<OrphanedUser, Long> {

    /**
     * Find all orphaned users with PENDING status.
     */
    List<OrphanedUser> findByStatus(OrphanedUser.OrphanedUserStatus status);

    /**
     * Find orphaned user by userId.
     */
    boolean existsByUserId(UUID userId);
}
