package com.marketplace.catalog.domain.repository;

import com.marketplace.catalog.domain.enums.ProductStatus;
import com.marketplace.catalog.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    Page<Product> findBySellerId(UUID sellerId, Pageable pageable);
    
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:sellerId IS NULL OR p.sellerId = :sellerId) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Product> findByFilters(
        @Param("categoryId") Long categoryId,
        @Param("sellerId") UUID sellerId,
        @Param("status") ProductStatus status,
        Pageable pageable
    );
    
    Page<Product> findByFeaturedTrue(Pageable pageable);
}
