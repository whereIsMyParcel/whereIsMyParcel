package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentInfoResponse(
        UUID id,
        UUID orderId,
        UUID originHubId,
        UUID currentHubId,
        UUID destinationHubId,
        UUID companyDeliveryManagerId,
        String shipmentNumber,
        String shipmentStatus,
        String deliveryAddress,
        String recipientName,
        String recipientSlackId,
        LocalDateTime estimatedDeliveryAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt
) {
    public static ShipmentInfoResponse from(Shipment shipment) {
        return new ShipmentInfoResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getOriginHubId(),
                shipment.getCurrentHubId(),
                shipment.getDestinationHubId(),
                shipment.getCompanyDeliveryManagerId(),
                shipment.getShipmentNumber(),
                shipment.getShipmentStatus().name(),
                shipment.getDeliveryAddress(),
                shipment.getRecipientName(),
                shipment.getRecipientSlackId(),
                shipment.getEstimatedDeliveryAt(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt()
        );
    }
}
