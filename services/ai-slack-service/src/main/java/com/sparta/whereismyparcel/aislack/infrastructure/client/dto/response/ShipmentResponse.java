package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentResponse(
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
}
