package com.marketplace.user.domain.repository;

import com.marketplace.user.domain.model.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for UserPreferences entity.
 */
@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
}
