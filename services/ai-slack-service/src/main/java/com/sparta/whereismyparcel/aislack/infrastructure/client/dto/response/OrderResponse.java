package com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String orderNumber,
        String recipientName,
        String recipientAddress,
        LocalDateTime requestedDeliveryAt,
        String orderStatus,
        String requestMemo,
        LocalDateTime orderedAt,
        List<OrderShipmentItem> shipmentItems
) {
    public record OrderShipmentItem(
            UUID shipmentId,
            List<String> items
    ) {}
}
