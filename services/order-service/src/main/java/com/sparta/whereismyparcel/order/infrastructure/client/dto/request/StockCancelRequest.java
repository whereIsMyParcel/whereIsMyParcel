package com.sparta.whereismyparcel.order.infrastructure.client.dto.request;

import java.util.List;
import java.util.UUID;

public record StockCancelRequest(
        UUID orderId,
        List<Item> items
) {
    public record Item(UUID skuId, Integer quantity) {}
}
