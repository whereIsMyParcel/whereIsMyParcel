package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import com.sparta.whereismyparcel.shipment.domain.entity.ShipmentHistory;

import java.util.List;
import java.util.UUID;

public record ShipmentCreateResponse(
        UUID shipmentId,
        UUID orderId,
        UUID hubId,
        UUID destinationHubId,
        String status,
        String trackingNumber,
        String address,
        String recipientName,
        String recipientPhone,
        List<History> histories
) {

    public record History(
            UUID originHubId,
            int sequence,
            UUID destinationHubId,
            UUID hubDeliveryManagerId,
            String status,
            int estimatedDuration,
            int actualDuration,
            String description
    ) {
        public static History from(ShipmentHistory entity) {
            return new History(
                    entity.getOriginHubId(),
                    entity.getSequence(),
                    entity.getDestinationHubId(),
                    entity.getHubDeliveryManagerId(),
                    entity.getStatus().name(),
                    entity.getEstimatedDuration(),
                    entity.getActualDuration(),
                    entity.getDescription()
            );
        }
    }

    public static ShipmentCreateResponse from(Shipment shipment) {
        return new ShipmentCreateResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getOriginHubId(),
                shipment.getDestinationHubId(),
                shipment.getShipmentStatus().name(),
                shipment.getShipmentNumber(),
                shipment.getDeliveryAddress(),
                shipment.getRecipientName(),
                shipment.getRecipientSlackId(),
                shipment.getHistories()
                        .stream()
                        .map(History::from)
                        .toList()
        );
    }
}