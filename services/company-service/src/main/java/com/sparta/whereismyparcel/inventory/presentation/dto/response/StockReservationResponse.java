package com.sparta.whereismyparcel.inventory.presentation.dto.response;

import java.util.UUID;

public record StockReservationResponse(
        UUID productVariantId,
        Integer reservedQuantity
) {
}
