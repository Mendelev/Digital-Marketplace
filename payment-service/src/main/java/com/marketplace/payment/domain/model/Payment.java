package com.marketplace.payment.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String provider = "MOCK";

    @Column(name = "captured_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal capturedAmount = BigDecimal.ZERO;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public Payment() {
    }

    public Payment(UUID orderId, UUID userId, BigDecimal amount, String currency) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.INITIATED;
        this.capturedAmount = BigDecimal.ZERO;
        this.refundedAmount = BigDecimal.ZERO;
    }

    // Business methods
    public void authorize() {
        validateTransition(PaymentStatus.AUTHORIZED);
        this.status = PaymentStatus.AUTHORIZED;
    }

    public void capture(BigDecimal captureAmount) {
        if (!canCapture(captureAmount)) {
            throw new IllegalStateException("Cannot capture payment: invalid state or amount exceeds authorized amount");
        }
        this.capturedAmount = this.capturedAmount.add(captureAmount);

        // If fully captured, change status
        if (this.capturedAmount.compareTo(this.amount) == 0) {
            this.status = PaymentStatus.CAPTURED;
        }
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void refund(BigDecimal refundAmount) {
        if (!canRefund(refundAmount)) {
            throw new IllegalStateException("Cannot refund payment: invalid state or amount exceeds captured amount");
        }
        this.refundedAmount = this.refundedAmount.add(refundAmount);

        // If fully refunded, change status
        if (this.refundedAmount.compareTo(this.capturedAmount) == 0) {
            this.status = PaymentStatus.REFUNDED;
        }
    }

    public void voidAuthorization() {
        validateTransition(PaymentStatus.VOIDED);
        this.status = PaymentStatus.VOIDED;
    }

    public boolean canCapture(BigDecimal captureAmount) {
        if (status != PaymentStatus.AUTHORIZED && status != PaymentStatus.CAPTURED) {
            return false;
        }
        BigDecimal newCapturedTotal = capturedAmount.add(captureAmount);
        return newCapturedTotal.compareTo(amount) <= 0;
    }

    public boolean canRefund(BigDecimal refundAmount) {
        if (status != PaymentStatus.CAPTURED && status != PaymentStatus.REFUNDED) {
            return false;
        }
        BigDecimal newRefundedTotal = refundedAmount.add(refundAmount);
        return newRefundedTotal.compareTo(capturedAmount) <= 0;
    }

    public boolean canVoid() {
        return status == PaymentStatus.AUTHORIZED && capturedAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isFullyCaptured() {
        return capturedAmount.compareTo(amount) == 0;
    }

    public boolean isFullyRefunded() {
        return refundedAmount.compareTo(capturedAmount) == 0;
    }

    public BigDecimal getRemainingAuthAmount() {
        return amount.subtract(capturedAmount);
    }

    public BigDecimal getRemainingRefundAmount() {
        return capturedAmount.subtract(refundedAmount);
    }

    private void validateTransition(PaymentStatus targetStatus) {
        switch (targetStatus) {
            case AUTHORIZED:
                if (status != PaymentStatus.INITIATED) {
                    throw new IllegalStateException(
                        String.format("Cannot transition from %s to AUTHORIZED", status));
                }
                break;
            case VOIDED:
                if (status != PaymentStatus.AUTHORIZED) {
                    throw new IllegalStateException(
                        String.format("Cannot transition from %s to VOIDED. Only AUTHORIZED payments can be voided", status));
                }
                if (capturedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    throw new IllegalStateException("Cannot void payment with captured amount");
                }
                break;
            default:
                break;
        }
    }

    // Getters and setters
    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public BigDecimal getCapturedAmount() {
        return capturedAmount;
    }

    public void setCapturedAmount(BigDecimal capturedAmount) {
        this.capturedAmount = capturedAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
