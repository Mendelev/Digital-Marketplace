package com.marketplace.order.domain.repository;

import com.marketplace.order.domain.model.Reservation;
import com.marketplace.order.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Reservation entity.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    /**
     * Find reservation by order ID.
     */
    Optional<Reservation> findByOrderId(UUID orderId);

    /**
     * Find active reservations that have expired.
     */
    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.expiresAt < :now")
    List<Reservation> findExpiredReservations(ReservationStatus status, OffsetDateTime now);
}
