package com.marketplace.auth.domain.repository;

import com.marketplace.auth.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token hash.
     *
     * @param tokenHash the BCrypt hash of the token
     * @return Optional containing the refresh token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Delete all expired or revoked tokens older than the specified date.
     *
     * @param olderThan the cutoff date
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE " +
           "(rt.revokedAt IS NOT NULL OR rt.expiresAt < :now) " +
           "AND rt.createdAt < :olderThan")
    int deleteExpiredAndRevoked(@Param("now") LocalDateTime now, 
                                 @Param("olderThan") LocalDateTime olderThan);

    /**
     * Delete all tokens (including active) older than the specified date.
     *
     * @param olderThan the cutoff date
     * @return number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.createdAt < :olderThan")
    int deleteAllOlderThan(@Param("olderThan") LocalDateTime olderThan);
}
