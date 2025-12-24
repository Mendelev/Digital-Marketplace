package com.marketplace.payment.service;

import com.marketplace.payment.config.PaymentSimulationConfig;
import com.marketplace.payment.domain.model.TransactionStatus;
import com.marketplace.payment.domain.model.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 * Service to simulate payment gateway behavior.
 * Configurable success/failure rates for testing different scenarios.
 */
@Service
public class PaymentSimulationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSimulationService.class);
    private final PaymentSimulationConfig config;
    private final Random random = new Random();

    public PaymentSimulationService(PaymentSimulationConfig config) {
        this.config = config;
    }

    /**
     * Simulate payment authorization.
     */
    public SimulationResult simulateAuthorization() {
        return simulateOperation(TransactionType.AUTHORIZE, config.authorizationSuccessRate());
    }

    /**
     * Simulate payment capture.
     */
    public SimulationResult simulateCapture() {
        return simulateOperation(TransactionType.CAPTURE, config.captureSuccessRate());
    }

    /**
     * Simulate payment refund.
     */
    public SimulationResult simulateRefund() {
        return simulateOperation(TransactionType.REFUND, config.refundSuccessRate());
    }

    /**
     * Simulate payment void.
     */
    public SimulationResult simulateVoid() {
        return simulateOperation(TransactionType.VOID, config.voidSuccessRate());
    }

    /**
     * Generic simulation logic with configurable success rate.
     */
    private SimulationResult simulateOperation(TransactionType type, double successRate) {
        // Simulate network delay
        if (config.simulationDelayMs() > 0) {
            try {
                Thread.sleep(config.simulationDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Simulation delay interrupted", e);
            }
        }

        boolean success = random.nextDouble() < successRate;
        String providerReference = "mock-txn-" + UUID.randomUUID().toString().substring(0, 8);

        TransactionStatus status = success ? TransactionStatus.SUCCESS : TransactionStatus.FAILED;
        String errorMessage = success ? null : getRandomErrorMessage(type);

        log.debug("Simulated {} operation: status={}, reference={}",
                  type, status, providerReference);

        return new SimulationResult(status, providerReference, errorMessage);
    }

    /**
     * Generate realistic error messages for failed transactions.
     */
    private String getRandomErrorMessage(TransactionType type) {
        String[] authErrors = {
            "Insufficient funds",
            "Card declined",
            "Invalid card number",
            "Card expired",
            "Security check failed"
        };

        String[] captureErrors = {
            "Authorization expired",
            "Capture amount exceeds authorized amount",
            "Provider timeout",
            "Duplicate capture attempt"
        };

        String[] refundErrors = {
            "Refund window expired",
            "Insufficient balance for refund",
            "Original transaction not found",
            "Provider error"
        };

        String[] voidErrors = {
            "Authorization already captured",
            "Void window expired",
            "Provider timeout"
        };

        String[] errors = switch (type) {
            case AUTHORIZE -> authErrors;
            case CAPTURE -> captureErrors;
            case REFUND -> refundErrors;
            case VOID -> voidErrors;
        };

        return errors[random.nextInt(errors.length)];
    }

    /**
     * Simulation result record.
     */
    public record SimulationResult(
        TransactionStatus status,
        String providerReference,
        String errorMessage
    ) {
        public boolean isSuccess() {
            return status == TransactionStatus.SUCCESS;
        }
    }
}
