package com.marketplace.payment.domain.repository;

import com.marketplace.payment.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}
