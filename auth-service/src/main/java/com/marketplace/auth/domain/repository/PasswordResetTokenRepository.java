package com.marketplace.auth.domain.repository;

import com.marketplace.auth.domain.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PasswordResetToken entity operations.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find password reset token by token hash.
     *
     * @param tokenHash the BCrypt hash of the token
     * @return Optional containing the password reset token if found
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Find the most recent valid password reset token for an email.
     *
     * @param email the email address
     * @return Optional containing the latest password reset token if found
     */
    Optional<PasswordResetToken> findFirstByEmailOrderByCreatedAtDesc(String email);
}
