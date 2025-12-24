package com.marketplace.cart.domain.repository;

import com.marketplace.cart.domain.model.Cart;
import com.marketplace.cart.domain.model.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Cart entity.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * Find active cart for a user.
     */
    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    /**
     * Check if user has an active cart.
     */
    boolean existsByUserIdAndStatus(UUID userId, CartStatus status);
}
