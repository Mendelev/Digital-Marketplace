package com.marketplace.inventory.domain.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
@EntityListeners(AuditingEntityListener.class)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "movement_id")
    private UUID movementId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "previous_available_qty", nullable = false)
    private Integer previousAvailableQty;

    @Column(name = "new_available_qty", nullable = false)
    private Integer newAvailableQty;

    @Column(name = "previous_reserved_qty", nullable = false)
    private Integer previousReservedQty;

    @Column(name = "new_reserved_qty", nullable = false)
    private Integer newReservedQty;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected StockMovement() {
        // JPA requires a no-arg constructor
    }

    public StockMovement(String sku, MovementType movementType, Integer quantity,
                        Integer previousAvailableQty, Integer newAvailableQty,
                        Integer previousReservedQty, Integer newReservedQty,
                        UUID reservationId, String reason, String createdBy) {
        this.sku = sku;
        this.movementType = movementType;
        this.quantity = quantity;
        this.previousAvailableQty = previousAvailableQty;
        this.newAvailableQty = newAvailableQty;
        this.previousReservedQty = previousReservedQty;
        this.newReservedQty = newReservedQty;
        this.reservationId = reservationId;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    // Getters and setters

    public UUID getMovementId() {
        return movementId;
    }

    public void setMovementId(UUID movementId) {
        this.movementId = movementId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPreviousAvailableQty() {
        return previousAvailableQty;
    }

    public void setPreviousAvailableQty(Integer previousAvailableQty) {
        this.previousAvailableQty = previousAvailableQty;
    }

    public Integer getNewAvailableQty() {
        return newAvailableQty;
    }

    public void setNewAvailableQty(Integer newAvailableQty) {
        this.newAvailableQty = newAvailableQty;
    }

    public Integer getPreviousReservedQty() {
        return previousReservedQty;
    }

    public void setPreviousReservedQty(Integer previousReservedQty) {
        this.previousReservedQty = previousReservedQty;
    }

    public Integer getNewReservedQty() {
        return newReservedQty;
    }

    public void setNewReservedQty(Integer newReservedQty) {
        this.newReservedQty = newReservedQty;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
