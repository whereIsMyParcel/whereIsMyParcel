package com.sparta.whereismyparcel.inventory.presentation.dto.request;

import java.util.UUID;

public record InventoryStockRequest(
        UUID productVariantId,
        UUID hubId,
        Integer quantity
) {
}
