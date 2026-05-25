package com.sparta.whereismyparcel.inventory.presentation.dto.response;

import com.sparta.whereismyparcel.inventory.domain.entity.Inventory;

import java.util.UUID;

public record AddInventoryResponse(
        UUID hubId,
        UUID productVariantId,
        Integer quantity
) {
    public static AddInventoryResponse from(Inventory inventory) {
        return new AddInventoryResponse(
                inventory.getHubId(),
                inventory.getProductVariant().getId(),
                inventory.getQuantity()
        );
    }
}
