package com.marketplace.inventory.domain.model;

import com.marketplace.inventory.exception.InvalidReservationStateException;
import com.marketplace.inventory.exception.ReservationExpiredException;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations")
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReservationLine> lines = new ArrayList<>();

    protected Reservation() {
        // JPA requires a no-arg constructor
    }

    public Reservation(UUID orderId, OffsetDateTime expiresAt) {
        this.orderId = orderId;
        this.status = ReservationStatus.ACTIVE;
        this.expiresAt = expiresAt;
    }

    // Business logic methods

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public void confirm() {
        if (isExpired()) {
            throw new ReservationExpiredException(reservationId);
        }
        if (status != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(reservationId, status);
        }
        status = ReservationStatus.CONFIRMED;
    }

    public void release() {
        if (status == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException(reservationId, status);
        }
        status = ReservationStatus.RELEASED;
    }

    public void expire() {
        if (status == ReservationStatus.ACTIVE) {
            status = ReservationStatus.EXPIRED;
        }
    }

    public void addLine(ReservationLine line) {
        lines.add(line);
        line.setReservation(this);
    }

    // Getters and setters

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    public List<ReservationLine> getLines() {
        return lines;
    }

    public void setLines(List<ReservationLine> lines) {
        this.lines = lines;
    }
}
