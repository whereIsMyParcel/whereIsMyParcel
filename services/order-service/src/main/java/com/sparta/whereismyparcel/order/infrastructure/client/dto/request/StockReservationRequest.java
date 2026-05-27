package com.sparta.whereismyparcel.order.infrastructure.client.dto.request;

import java.util.List;
import java.util.UUID;

public record StockReservationRequest(
        UUID orderId,
        List<Item> items
) {
    public record Item(String skuCode, Integer quantity) {}
}
