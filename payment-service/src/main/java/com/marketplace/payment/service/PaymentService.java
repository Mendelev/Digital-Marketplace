package com.marketplace.payment.service;

import com.marketplace.payment.domain.model.*;
import com.marketplace.payment.domain.repository.PaymentRepository;
import com.marketplace.payment.domain.repository.PaymentTransactionRepository;
import com.marketplace.payment.exception.InvalidPaymentStateException;
import com.marketplace.payment.exception.PaymentAlreadyProcessedException;
import com.marketplace.payment.exception.PaymentNotFoundException;
import com.marketplace.payment.exception.PaymentProcessingException;
import com.marketplace.shared.dto.payment.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core payment service handling payment lifecycle.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentSimulationService simulationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository transactionRepository,
            PaymentSimulationService simulationService) {
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.simulationService = simulationService;
    }

    /**
     * Create a new payment intent.
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        MDC.put("operation", "createPayment");
        MDC.put("orderId", request.orderId().toString());
        MDC.put("userId", request.userId().toString());

        log.info("Creating payment for order: orderId={}, amount={} {}",
                 request.orderId(), request.amount(), request.currency());

        // Check for existing payment for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(request.orderId());
        if (existingPayment.isPresent()) {
            log.warn("Payment already exists for order: orderId={}", request.orderId());
            throw new PaymentAlreadyProcessedException(
                "Payment already exists for order: " + request.orderId());
        }

        // Create payment
        Payment payment = new Payment(
            request.orderId(),
            request.userId(),
            request.amount(),
            request.currency()
        );

        payment = paymentRepository.save(payment);
        MDC.put("paymentId", payment.getPaymentId().toString());

        log.info("Payment created successfully: paymentId={}", payment.getPaymentId());

        return mapToResponse(payment);
    }

    /**
     * Authorize payment.
     */
    @Transactional
    public PaymentResponse authorizePayment(UUID paymentId, AuthorizePaymentRequest request) {
        MDC.put("operation", "authorizePayment");
        MDC.put("paymentId", paymentId.toString());

        log.info("Authorizing payment: paymentId={}", paymentId);

        // Check for idempotency
        if (request.idempotencyKey() != null) {
            Optional<PaymentTransaction> existingTxn =
                transactionRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existingTxn.isPresent()) {
                log.info("Idempotent request detected, returning existing authorization");
                Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
                return mapToResponse(payment);
            }
        }

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Validate state
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new InvalidPaymentStateException(
                "Payment is not in INITIATED state: " + payment.getStatus());
        }

        // Simulate authorization
        PaymentSimulationService.SimulationResult result = simulationService.simulateAuthorization();

        // Create transaction record
        PaymentTransaction transaction = new PaymentTransaction(
            paymentId,
            TransactionType.AUTHORIZE,
            result.status(),
            payment.getAmount(),
            payment.getCurrency()
        );
        transaction.setProviderReference(result.providerReference());
        transaction.setErrorMessage(result.errorMessage());
        transaction.setIdempotencyKey(request.idempotencyKey());

        transactionRepository.save(transaction);

        if (result.isSuccess()) {
            payment.authorize();
            payment = paymentRepository.save(payment);
            log.info("Payment authorized successfully: paymentId={}", paymentId);
            // TODO: Emit PaymentAuthorized event
        } else {
            payment.markFailed();
            payment = paymentRepository.save(payment);
            log.warn("Payment authorization failed: paymentId={}, reason={}",
                     paymentId, result.errorMessage());
            // TODO: Emit PaymentFailed event
            throw new PaymentProcessingException("Payment authorization failed: " + result.errorMessage());
        }

        return mapToResponse(payment);
    }

    /**
     * Capture payment (full or partial).
     */
    @Transactional
    public PaymentResponse capturePayment(UUID paymentId, CapturePaymentRequest request) {
        MDC.put("operation", "capturePayment");
        MDC.put("paymentId", paymentId.toString());

        log.info("Capturing payment: paymentId={}, amount={}", paymentId, request.amount());

        // Check for idempotency
        if (request.idempotencyKey() != null) {
            Optional<PaymentTransaction> existingTxn =
                transactionRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existingTxn.isPresent()) {
                log.info("Idempotent request detected, returning existing capture");
                Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
                return mapToResponse(payment);
            }
        }

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Validate capture is allowed
        if (!payment.canCapture(request.amount())) {
            throw new InvalidPaymentStateException(
                String.format("Cannot capture amount %s. Status: %s, Captured: %s, Authorized: %s",
                    request.amount(), payment.getStatus(), payment.getCapturedAmount(), payment.getAmount()));
        }

        // Simulate capture
        PaymentSimulationService.SimulationResult result = simulationService.simulateCapture();

        // Create transaction record
        PaymentTransaction transaction = new PaymentTransaction(
            paymentId,
            TransactionType.CAPTURE,
            result.status(),
            request.amount(),
            payment.getCurrency()
        );
        transaction.setProviderReference(result.providerReference());
        transaction.setErrorMessage(result.errorMessage());
        transaction.setIdempotencyKey(request.idempotencyKey());

        transactionRepository.save(transaction);

        if (result.isSuccess()) {
            payment.capture(request.amount());
            payment = paymentRepository.save(payment);
            log.info("Payment captured successfully: paymentId={}, amount={}, totalCaptured={}",
                     paymentId, request.amount(), payment.getCapturedAmount());
            // TODO: Emit PaymentCaptured event
        } else {
            log.warn("Payment capture failed: paymentId={}, reason={}",
                     paymentId, result.errorMessage());
            // TODO: Emit PaymentFailed event
            throw new PaymentProcessingException("Payment capture failed: " + result.errorMessage());
        }

        return mapToResponse(payment);
    }

    /**
     * Refund payment (full or partial).
     */
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, RefundPaymentRequest request) {
        MDC.put("operation", "refundPayment");
        MDC.put("paymentId", paymentId.toString());

        log.info("Refunding payment: paymentId={}, amount={}, reason={}",
                 paymentId, request.amount(), request.reason());

        // Check for idempotency
        if (request.idempotencyKey() != null) {
            Optional<PaymentTransaction> existingTxn =
                transactionRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existingTxn.isPresent()) {
                log.info("Idempotent request detected, returning existing refund");
                Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
                return mapToResponse(payment);
            }
        }

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Validate refund is allowed
        if (!payment.canRefund(request.amount())) {
            throw new InvalidPaymentStateException(
                String.format("Cannot refund amount %s. Status: %s, Refunded: %s, Captured: %s",
                    request.amount(), payment.getStatus(), payment.getRefundedAmount(), payment.getCapturedAmount()));
        }

        // Simulate refund
        PaymentSimulationService.SimulationResult result = simulationService.simulateRefund();

        // Create transaction record
        PaymentTransaction transaction = new PaymentTransaction(
            paymentId,
            TransactionType.REFUND,
            result.status(),
            request.amount(),
            payment.getCurrency()
        );
        transaction.setProviderReference(result.providerReference());
        transaction.setErrorMessage(result.errorMessage());
        transaction.setIdempotencyKey(request.idempotencyKey());

        transactionRepository.save(transaction);

        if (result.isSuccess()) {
            payment.refund(request.amount());
            payment = paymentRepository.save(payment);
            log.info("Payment refunded successfully: paymentId={}, amount={}, totalRefunded={}",
                     paymentId, request.amount(), payment.getRefundedAmount());
            // TODO: Emit PaymentRefunded event
        } else {
            log.warn("Payment refund failed: paymentId={}, reason={}",
                     paymentId, result.errorMessage());
            throw new PaymentProcessingException("Payment refund failed: " + result.errorMessage());
        }

        return mapToResponse(payment);
    }

    /**
     * Void payment authorization.
     */
    @Transactional
    public PaymentResponse voidPayment(UUID paymentId, VoidPaymentRequest request) {
        MDC.put("operation", "voidPayment");
        MDC.put("paymentId", paymentId.toString());

        log.info("Voiding payment: paymentId={}", paymentId);

        // Check for idempotency
        if (request.idempotencyKey() != null) {
            Optional<PaymentTransaction> existingTxn =
                transactionRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existingTxn.isPresent()) {
                log.info("Idempotent request detected, returning existing void");
                Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException(paymentId));
                return mapToResponse(payment);
            }
        }

        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Validate void is allowed
        if (!payment.canVoid()) {
            throw new InvalidPaymentStateException(
                String.format("Cannot void payment. Status: %s, Captured amount: %s",
                    payment.getStatus(), payment.getCapturedAmount()));
        }

        // Simulate void
        PaymentSimulationService.SimulationResult result = simulationService.simulateVoid();

        // Create transaction record
        PaymentTransaction transaction = new PaymentTransaction(
            paymentId,
            TransactionType.VOID,
            result.status(),
            payment.getAmount(),
            payment.getCurrency()
        );
        transaction.setProviderReference(result.providerReference());
        transaction.setErrorMessage(result.errorMessage());
        transaction.setIdempotencyKey(request.idempotencyKey());

        transactionRepository.save(transaction);

        if (result.isSuccess()) {
            payment.voidAuthorization();
            payment = paymentRepository.save(payment);
            log.info("Payment voided successfully: paymentId={}", paymentId);
            // TODO: Emit PaymentVoided event
        } else {
            log.warn("Payment void failed: paymentId={}, reason={}",
                     paymentId, result.errorMessage());
            throw new PaymentProcessingException("Payment void failed: " + result.errorMessage());
        }

        return mapToResponse(payment);
    }

    /**
     * Get payment by ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return mapToResponse(payment);
    }

    /**
     * Get payment by order ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
        return mapToResponse(payment);
    }

    /**
     * Get all payments for a user.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUser(UUID userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get transaction history for a payment.
     */
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getPaymentTransactions(UUID paymentId) {
        // Verify payment exists
        if (!paymentRepository.existsById(paymentId)) {
            throw new PaymentNotFoundException(paymentId);
        }

        List<PaymentTransaction> transactions =
            transactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);

        return transactions.stream()
            .map(this::mapTransactionToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Map Payment entity to PaymentResponse DTO.
     */
    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getStatus().name(),
            payment.getAmount(),
            payment.getCapturedAmount(),
            payment.getRefundedAmount(),
            payment.getCurrency(),
            payment.getProvider(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }

    /**
     * Map PaymentTransaction entity to PaymentTransactionResponse DTO.
     */
    private PaymentTransactionResponse mapTransactionToResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
            transaction.getTransactionId(),
            transaction.getPaymentId(),
            transaction.getType().name(),
            transaction.getStatus().name(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getProviderReference(),
            transaction.getErrorMessage(),
            transaction.getCreatedAt()
        );
    }
}
