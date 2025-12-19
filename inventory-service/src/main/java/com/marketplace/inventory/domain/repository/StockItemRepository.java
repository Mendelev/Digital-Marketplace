package com.marketplace.inventory.domain.repository;

import com.marketplace.inventory.domain.model.StockItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, String>, JpaSpecificationExecutor<StockItem> {

    Optional<StockItem> findByProductId(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockItem s WHERE s.sku = :sku")
    Optional<StockItem> findBySkuForUpdate(@Param("sku") String sku);

    @Query("SELECT s FROM StockItem s WHERE s.availableQty <= s.lowStockThreshold")
    List<StockItem> findLowStockItems();

    @Query("SELECT s FROM StockItem s WHERE s.availableQty <= s.lowStockThreshold")
    Page<StockItem> findLowStockItems(Pageable pageable);
}
