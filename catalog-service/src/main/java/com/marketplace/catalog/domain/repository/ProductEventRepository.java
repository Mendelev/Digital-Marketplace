package com.marketplace.catalog.domain.repository;

import com.marketplace.catalog.domain.model.ProductEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductEventRepository extends JpaRepository<ProductEvent, Long> {
    
    boolean existsByEventId(UUID eventId);
    
    @Query("SELECT MAX(e.sequenceNumber) FROM ProductEvent e")
    Long findMaxSequenceNumber();
}
