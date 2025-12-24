package com.marketplace.order.service;

import com.marketplace.order.config.OrderServiceProperties;
import com.marketplace.order.domain.model.*;
import com.marketplace.order.domain.repository.PaymentRepository;
import com.marketplace.order.domain.repository.PaymentTransactionRepository;
import com.marketplace.order.dto.PaymentResponse;
import com.marketplace.order.exception.PaymentFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Mock payment service for simulating payment operations.
 * In production, this would integrate with a real payment gateway.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final OrderServiceProperties properties;
    private final Random random = new Random();

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentTransactionRepository transactionRepository,
                          OrderServiceProperties properties) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    /**
     * Authorize payment for an order.
     * Simulates payment authorization with configurable success rate.
     *
     * @return PaymentResponse with payment details
     * @throws PaymentFailedException if authorization fails
     */
    @Transactional
    public PaymentResponse authorizePayment(UUID orderId, UUID userId, BigDecimal amount, String currency) {
        log.info("Authorizing payment for order: {}, amount: {} {}", orderId, amount, currency);

        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.INITIATED);

        Payment savedPayment = paymentRepository.save(payment);

        // Simulate payment authorization (configurable success rate)
        boolean success = random.nextInt(100) < properties.paymentSuccessRate();

        if (success) {
            // Authorization successful
            savedPayment.setStatus(PaymentStatus.AUTHORIZED);
            paymentRepository.save(savedPayment);

            // Record transaction
            recordTransaction(savedPayment.getPaymentId(), TransactionType.AUTHORIZE,
                    TransactionStatus.SUCCESS, amount, "MOCK-AUTH-" + UUID.randomUUID());

            log.info("Payment authorized successfully: {}", savedPayment.getPaymentId());

            return new PaymentResponse(
                    savedPayment.getPaymentId(),
                    PaymentStatus.AUTHORIZED.name(),
                    amount,
                    "Payment authorized successfully"
            );
        } else {
            // Authorization failed
            savedPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(savedPayment);

            // Record failed transaction
            recordTransaction(savedPayment.getPaymentId(), TransactionType.AUTHORIZE,
                    TransactionStatus.FAILED, amount, null);

            log.warn("Payment authorization failed for order: {}", orderId);

            throw new PaymentFailedException("Payment authorization failed - insufficient funds or card declined");
        }
    }

    /**
     * Capture authorized payment.
     * In a real system, this transfers funds from customer to merchant.
     */
    @Transactional
    public PaymentResponse capturePayment(UUID paymentId) {
        log.info("Capturing payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentFailedException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentFailedException("Cannot capture payment in status: " + payment.getStatus());
        }

        // Mock capture always succeeds if payment is authorized
        payment.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment);

        // Record transaction
        recordTransaction(paymentId, TransactionType.CAPTURE,
                TransactionStatus.SUCCESS, payment.getAmount(), "MOCK-CAPTURE-" + UUID.randomUUID());

        log.info("Payment captured successfully: {}", paymentId);

        return new PaymentResponse(
                paymentId,
                PaymentStatus.CAPTURED.name(),
                payment.getAmount(),
                "Payment captured successfully"
        );
    }

    /**
     * Refund captured payment.
     */
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {
        log.info("Refunding payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentFailedException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new PaymentFailedException("Cannot refund payment in status: " + payment.getStatus());
        }

        // Mock refund always succeeds
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // Record transaction
        recordTransaction(paymentId, TransactionType.REFUND,
                TransactionStatus.SUCCESS, payment.getAmount(), "MOCK-REFUND-" + UUID.randomUUID());

        log.info("Payment refunded successfully: {}", paymentId);

        return new PaymentResponse(
                paymentId,
                PaymentStatus.REFUNDED.name(),
                payment.getAmount(),
                "Payment refunded successfully"
        );
    }

    /**
     * Void authorized payment (cancel authorization before capture).
     */
    @Transactional
    public PaymentResponse voidPayment(UUID paymentId) {
        log.info("Voiding payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentFailedException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentFailedException("Cannot void payment in status: " + payment.getStatus());
        }

        // Mock void always succeeds
        payment.setStatus(PaymentStatus.VOIDED);
        paymentRepository.save(payment);

        // Record transaction
        recordTransaction(paymentId, TransactionType.VOID,
                TransactionStatus.SUCCESS, payment.getAmount(), "MOCK-VOID-" + UUID.randomUUID());

        log.info("Payment voided successfully: {}", paymentId);

        return new PaymentResponse(
                paymentId,
                PaymentStatus.VOIDED.name(),
                payment.getAmount(),
                "Payment authorization voided"
        );
    }

    /**
     * Record a payment transaction for audit trail.
     */
    private void recordTransaction(UUID paymentId, TransactionType type,
                                    TransactionStatus status, BigDecimal amount, String providerReference) {
        PaymentTransaction transaction = new PaymentTransaction(
                paymentId,
                type,
                status,
                amount,
                providerReference
        );
        transactionRepository.save(transaction);
    }
}
