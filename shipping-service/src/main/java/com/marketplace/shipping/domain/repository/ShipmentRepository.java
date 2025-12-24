package com.marketplace.shipping.domain.repository;

import com.marketplace.shipping.domain.model.Shipment;
import com.marketplace.shipping.domain.model.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByOrderId(UUID orderId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findByUserId(UUID userId);

    List<Shipment> findByStatus(ShipmentStatus status);

    List<Shipment> findByStatusIn(List<ShipmentStatus> statuses);
}
