package com.sparta.whereismyparcel.aislack.infrastructure.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID orderId,
        String deliveryAddress,
        String recipientName,
        String recipientSlackId,
        int shipmentNumber

) {
}
