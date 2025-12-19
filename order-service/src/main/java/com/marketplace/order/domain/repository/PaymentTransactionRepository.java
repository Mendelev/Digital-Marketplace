package com.marketplace.order.domain.repository;

import com.marketplace.order.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for PaymentTransaction entity.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    /**
     * Find all transactions for a payment.
     */
    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
