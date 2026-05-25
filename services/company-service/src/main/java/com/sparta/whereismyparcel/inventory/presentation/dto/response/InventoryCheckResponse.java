package com.sparta.whereismyparcel.inventory.presentation.dto.response;

import com.sparta.whereismyparcel.inventory.domain.entity.Inventory;

import java.util.UUID;

public record InventoryCheckResponse(
        UUID inventoryId,
        UUID hunId,
        UUID productVariantId,
        Integer availableQuantity
) {
    public static InventoryCheckResponse from (Inventory inventory) {
        return new InventoryCheckResponse(
                inventory.getInventoryId(),
                inventory.getHubId(),
                inventory.getProductVariant().getId(),
                inventory.getAvailableQuantity()
        );
    }
}
