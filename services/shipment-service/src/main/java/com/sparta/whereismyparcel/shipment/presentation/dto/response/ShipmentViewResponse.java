package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import com.sparta.whereismyparcel.shipment.domain.entity.Shipment;
import com.sparta.whereismyparcel.shipment.domain.entity.ShipmentHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ShipmentViewResponse(
        UUID shipmentId,
        UUID orderId,
        UUID originHubId,
        UUID currentHubId,
        UUID destinationHubId,
        String shipmentNumber,
        String status,
        String deliveryAddress,
        String recipientName,
        String recipientSlackId,
        LocalDateTime estimatedDeliveryAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        List<History> histories
) {

    public record History(
            UUID originHubId,
            int sequence,
            UUID destinationHubId,
            UUID hubDeliveryManagerId,
            String status,
            int estimatedDuration,
            int actualDistance,
            int actualDuration,
            String description
    ) {
        public static History from(ShipmentHistory history) {
            return new History(
                    history.getOriginHubId(),
                    history.getSequence(),
                    history.getDestinationHubId(),
                    history.getHubDeliveryManagerId(),
                    history.getStatus().name(),
                    history.getEstimatedDuration(),
                    history.getActualDistance(),
                    history.getActualDuration(),
                    history.getDescription()
            );
        }
    }

    public static ShipmentViewResponse from(Shipment shipment) {
        return new ShipmentViewResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getOriginHubId(),
                shipment.getCurrentHubId(),
                shipment.getDestinationHubId(),
                shipment.getShipmentNumber(),
                shipment.getShipmentStatus().name(),
                shipment.getDeliveryAddress(),
                shipment.getRecipientName(),
                shipment.getRecipientSlackId(),
                shipment.getEstimatedDeliveryAt(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt(),
                shipment.getHistories()
                        .stream()
                        .map(History::from)
                        .toList()
        );
    }
}
