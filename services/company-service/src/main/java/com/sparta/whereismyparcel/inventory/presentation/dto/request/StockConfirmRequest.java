package com.sparta.whereismyparcel.inventory.presentation.dto.request;

import java.util.UUID;

public record StockConfirmRequest(
        UUID productVariantId,
        Integer quantity
) {
}
