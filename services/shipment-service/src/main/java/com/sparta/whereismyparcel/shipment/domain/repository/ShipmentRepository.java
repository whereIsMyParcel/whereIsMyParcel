package com.sparta.whereismyparcel.shipment.domain.repository;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.ShipmentSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    List<Shipment> findAllByOrderId(UUID orderId);


    @Query("""
                SELECT s FROM Shipment s
                WHERE (:#{#req.orderId} IS NULL OR s.orderId = :#{#req.orderId})
                  AND (:#{#req.originHubId} IS NULL OR s.originHubId = :#{#req.originHubId})
                  AND (:#{#req.currentHubId} IS NULL OR s.currentHubId = :#{#req.currentHubId})
                  AND (:#{#req.destinationHubId} IS NULL OR s.destinationHubId = :#{#req.destinationHubId})
                  AND (:#{#req.companyDeliveryManagerId} IS NULL OR s.companyDeliveryManagerId = :#{#req.companyDeliveryManagerId})
                  AND (:#{#req.shipmentNumber} IS NULL OR s.shipmentNumber = :#{#req.shipmentNumber})
                  AND (:#{#req.shipmentStatus} IS NULL OR s.shipmentStatus = :#{#req.shipmentStatus})
                  AND (:#{#req.deliveryAddress} IS NULL OR s.deliveryAddress LIKE %:#{#req.deliveryAddress}%)
                  AND (:#{#req.recipientName} IS NULL OR s.recipientName LIKE %:#{#req.recipientName}%)
                  AND (:#{#req.recipientSlackId} IS NULL OR s.recipientSlackId = :#{#req.recipientSlackId})
            """)
    Page<Shipment> search(@Param("req") ShipmentSearchRequest request, Pageable pageable);
}
