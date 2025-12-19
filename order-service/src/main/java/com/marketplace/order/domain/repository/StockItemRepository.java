package com.marketplace.order.domain.repository;

import com.marketplace.order.domain.model.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StockItem entity.
 */
@Repository
public interface StockItemRepository extends JpaRepository<StockItem, String> {

    /**
     * Find stock item by product ID.
     */
    Optional<StockItem> findByProductId(UUID productId);
}
