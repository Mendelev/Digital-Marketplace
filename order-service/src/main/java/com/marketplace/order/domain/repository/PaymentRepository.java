package com.marketplace.order.domain.repository;

import com.marketplace.order.domain.model.Payment;
import com.marketplace.order.domain.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find payment by order ID.
     */
    Optional<Payment> findByOrderId(UUID orderId);

    /**
     * Find all payments for a user.
     */
    List<Payment> findByUserId(UUID userId);

    /**
     * Find payments by status.
     */
    List<Payment> findByStatus(PaymentStatus status);
}
