package com.marketplace.order.domain.repository;

import com.marketplace.order.domain.model.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OrderEvent entity.
 */
@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    /**
     * Find all events for a specific order.
     */
    List<OrderEvent> findByOrderIdOrderBySequenceNumberAsc(UUID orderId);

    /**
     * Find event by event ID for idempotency check.
     */
    Optional<OrderEvent> findByEventId(UUID eventId);

    /**
     * Get the maximum sequence number across all events.
     */
    @Query("SELECT COALESCE(MAX(e.sequenceNumber), 0) FROM OrderEvent e")
    Long findMaxSequenceNumber();
}
