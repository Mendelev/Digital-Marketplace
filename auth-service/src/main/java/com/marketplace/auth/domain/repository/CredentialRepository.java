package com.marketplace.auth.domain.repository;

import com.marketplace.auth.domain.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Credential entity operations.
 */
@Repository
public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    /**
     * Find credential by email address.
     *
     * @param email the email address
     * @return Optional containing the credential if found
     */
    Optional<Credential> findByEmail(String email);

    /**
     * Find credential by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the credential if found
     */
    Optional<Credential> findByUserId(UUID userId);

    /**
     * Check if a credential exists with the given email.
     *
     * @param email the email address
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);
}
