package com.sparta.whereismyparcel.aislack.infrastructure.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String orderNumber,
        String recipientName,
        String recipientAddress,
        LocalDateTime requestedDeliveryAt,
        String orderStatus
) {
}
