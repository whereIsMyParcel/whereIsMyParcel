package com.sparta.whereismyparcel.shipment.domain.repository;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findAllByOrderId(UUID orderId);
}
