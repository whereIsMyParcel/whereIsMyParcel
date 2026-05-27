package com.sparta.whereismyparcel.order.infrastructure.client.dto.response;

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
    }
}
