package com.sparta.whereismyparcel.inventory.presentation.dto.request;

import java.util.UUID;

public record AddInventoryRequest(
        UUID productVariantId,
        UUID hubId,
        Integer quantity,
        Integer safetyStockQuantity
) {
}
