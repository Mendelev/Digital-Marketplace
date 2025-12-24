package com.marketplace.shipping.domain.repository;

import com.marketplace.shipping.domain.model.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {

    List<ShipmentTracking> findByShipmentIdOrderByEventTimeDesc(UUID shipmentId);

    Optional<ShipmentTracking> findFirstByShipmentIdOrderByEventTimeDesc(UUID shipmentId);
}
