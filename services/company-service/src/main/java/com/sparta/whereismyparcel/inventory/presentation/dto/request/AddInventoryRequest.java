package com.sparta.whereismyparcel.inventory.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record AddInventoryRequest(
        @NotNull
        UUID productVariantId,

        @NotNull
        @PositiveOrZero
        Integer quantity,

        @NotNull
        @PositiveOrZero
        Integer safetyStockQuantity
) {
}
