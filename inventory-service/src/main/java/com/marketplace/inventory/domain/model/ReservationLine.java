package com.marketplace.inventory.domain.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "reservation_lines")
public class ReservationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_line_id")
    private UUID reservationLineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    protected ReservationLine() {
        // JPA requires a no-arg constructor
    }

    public ReservationLine(String sku, Integer quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    // Getters and setters

    public UUID getReservationLineId() {
        return reservationLineId;
    }

    public void setReservationLineId(UUID reservationLineId) {
        this.reservationLineId = reservationLineId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
