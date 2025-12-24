package com.marketplace.inventory.domain.repository;

import com.marketplace.inventory.domain.model.Reservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' AND r.expiresAt <= :now ORDER BY r.expiresAt ASC")
    List<Reservation> findExpiredReservations(@Param("now") OffsetDateTime now, Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' AND r.expiresAt BETWEEN :now AND :threshold")
    List<Reservation> findExpiringSoon(@Param("now") OffsetDateTime now, @Param("threshold") OffsetDateTime threshold);
}
