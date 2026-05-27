package com.sparta.whereismyparcel.shipment.presentation.dto.request;

import java.util.UUID;

public record DecreaseInventoryRequest(
        UUID productVariantId,
        Integer quantity
) {}