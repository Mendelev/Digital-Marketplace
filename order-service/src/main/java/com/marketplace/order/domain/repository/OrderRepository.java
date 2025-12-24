package com.marketplace.order.domain.repository;

import com.marketplace.order.domain.model.Order;
import com.marketplace.order.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for Order entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find orders by user ID with pagination.
     */
    Page<Order> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find orders by user ID and status with pagination.
     */
    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    /**
     * Find orders by status with pagination.
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find order by payment ID.
     */
    Order findByPaymentId(UUID paymentId);
}
