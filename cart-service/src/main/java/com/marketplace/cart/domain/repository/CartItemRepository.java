package com.marketplace.cart.domain.repository;

import com.marketplace.cart.domain.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CartItem entity.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /**
     * Find cart item by cart and product ID (for duplicate detection).
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.cartId = :cartId AND ci.productId = :productId")
    Optional<CartItem> findByCartIdAndProductId(@Param("cartId") UUID cartId,
                                                  @Param("productId") UUID productId);

    /**
     * Find cart item by ID and ensure it belongs to the specified cart.
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cartItemId = :cartItemId AND ci.cart.cartId = :cartId")
    Optional<CartItem> findByCartItemIdAndCartId(@Param("cartItemId") UUID cartItemId,
                                                   @Param("cartId") UUID cartId);
}
