package com.sparta.whereismyparcel.inventory.presentation.dto.request;

import org.hibernate.cache.spi.support.AbstractReadWriteAccess;

import java.util.List;
import java.util.UUID;

public record StockReservationRequest(
        UUID orderId,
        List<Item> items
) {
    public record Item(
            UUID hubId,
            String skuCode,
            Integer quantity) {}
}
