package com.marketplace.shipping.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@EntityListeners(AuditingEntityListener.class)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shipment_id")
    private UUID shipmentId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentStatus status;

    @Column(name = "tracking_number", unique = true, length = 50)
    private String trackingNumber;

    @Column(nullable = false, length = 20)
    private String carrier = "MOCK";

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "package_weight_kg", precision = 10, scale = 3)
    private BigDecimal packageWeightKg;

    @Column(name = "package_dimensions", length = 50)
    private String packageDimensions;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    private AddressSnapshot shippingAddress;

    @Column(name = "estimated_delivery_date")
    private OffsetDateTime estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private OffsetDateTime actualDeliveryDate;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public Shipment() {
    }

    public Shipment(UUID orderId, UUID userId, AddressSnapshot shippingAddress,
                   Integer itemCount, BigDecimal shippingFee, String currency) {
        this.orderId = orderId;
        this.userId = userId;
        this.shippingAddress = shippingAddress;
        this.itemCount = itemCount;
        this.shippingFee = shippingFee;
        this.currency = currency;
        this.status = ShipmentStatus.PENDING;
        this.carrier = "MOCK";
    }

    // Business methods for state transitions
    public void markAsCreated() {
        validateTransition(ShipmentStatus.CREATED);
        this.status = ShipmentStatus.CREATED;
        this.trackingNumber = generateTrackingNumber();
        // Estimate delivery in 5 days
        this.estimatedDeliveryDate = OffsetDateTime.now().plusDays(5);
    }

    public void markAsInTransit() {
        validateTransition(ShipmentStatus.IN_TRANSIT);
        this.status = ShipmentStatus.IN_TRANSIT;
        this.shippedAt = OffsetDateTime.now();
    }

    public void markAsOutForDelivery() {
        validateTransition(ShipmentStatus.OUT_FOR_DELIVERY);
        this.status = ShipmentStatus.OUT_FOR_DELIVERY;
    }

    public void markAsDelivered() {
        validateTransition(ShipmentStatus.DELIVERED);
        this.status = ShipmentStatus.DELIVERED;
        this.deliveredAt = OffsetDateTime.now();
        this.actualDeliveryDate = OffsetDateTime.now();
    }

    public void markAsReturned() {
        // Admin-only operation, less strict validation
        if (this.status == ShipmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot mark a cancelled shipment as returned");
        }
        this.status = ShipmentStatus.RETURNED;
    }

    public void cancel() {
        if (!canBeCancelled()) {
            throw new IllegalStateException(
                "Cannot cancel shipment in status: " + status +
                ". Can only cancel PENDING or CREATED shipments."
            );
        }
        this.status = ShipmentStatus.CANCELLED;
    }

    public boolean canBeCancelled() {
        return status == ShipmentStatus.PENDING || status == ShipmentStatus.CREATED;
    }

    private void validateTransition(ShipmentStatus targetStatus) {
        switch (targetStatus) {
            case CREATED:
                if (status != ShipmentStatus.PENDING) {
                    throw new IllegalStateException(
                        "Cannot mark as CREATED from status: " + status
                    );
                }
                break;
            case IN_TRANSIT:
                if (status != ShipmentStatus.CREATED) {
                    throw new IllegalStateException(
                        "Cannot mark as IN_TRANSIT from status: " + status
                    );
                }
                break;
            case OUT_FOR_DELIVERY:
                if (status != ShipmentStatus.IN_TRANSIT) {
                    throw new IllegalStateException(
                        "Cannot mark as OUT_FOR_DELIVERY from status: " + status
                    );
                }
                break;
            case DELIVERED:
                if (status != ShipmentStatus.OUT_FOR_DELIVERY) {
                    throw new IllegalStateException(
                        "Cannot mark as DELIVERED from status: " + status
                    );
                }
                break;
            default:
                throw new IllegalStateException("Invalid target status: " + targetStatus);
        }
    }

    private String generateTrackingNumber() {
        return "TRACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters and setters
    public UUID getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(UUID shipmentId) {
        this.shipmentId = shipmentId;
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

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getPackageWeightKg() {
        return packageWeightKg;
    }

    public void setPackageWeightKg(BigDecimal packageWeightKg) {
        this.packageWeightKg = packageWeightKg;
    }

    public String getPackageDimensions() {
        return packageDimensions;
    }

    public void setPackageDimensions(String packageDimensions) {
        this.packageDimensions = packageDimensions;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public AddressSnapshot getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(AddressSnapshot shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public OffsetDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(OffsetDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public OffsetDateTime getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(OffsetDateTime actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public OffsetDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(OffsetDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(OffsetDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
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
