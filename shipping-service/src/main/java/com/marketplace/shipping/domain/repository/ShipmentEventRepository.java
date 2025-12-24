package com.marketplace.shipping.domain.repository;

import com.marketplace.shipping.domain.model.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {

    Optional<ShipmentEvent> findByEventId(UUID eventId);

    List<ShipmentEvent> findByShipmentIdOrderBySequenceNumberAsc(UUID shipmentId);

    @Query("SELECT COALESCE(MAX(e.sequenceNumber), 0) FROM ShipmentEvent e")
    Long findMaxSequenceNumber();
}
