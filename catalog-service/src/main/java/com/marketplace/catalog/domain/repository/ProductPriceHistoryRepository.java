package com.marketplace.catalog.domain.repository;

import com.marketplace.catalog.domain.model.ProductPriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {
    
    Page<ProductPriceHistory> findByProductId(UUID productId, Pageable pageable);
}
