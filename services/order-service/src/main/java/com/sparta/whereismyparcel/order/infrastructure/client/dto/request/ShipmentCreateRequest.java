package com.sparta.whereismyparcel.order.infrastructure.client.dto.request;

import java.util.List;
import java.util.UUID;

public record ShipmentCreateRequest(
        UUID orderId,
        String recipientName,
        String recipientPhone,
        String zipCode,
        String address,
        String addressDetail,
        List<Item> items
) {
    public record Item(UUID skuId, Integer quantity) {}
}
