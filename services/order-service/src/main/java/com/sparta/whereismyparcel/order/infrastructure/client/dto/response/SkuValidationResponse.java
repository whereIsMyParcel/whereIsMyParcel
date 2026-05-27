package com.sparta.whereismyparcel.order.infrastructure.client.dto.response;

import java.util.List;
import java.util.UUID;

public record SkuValidationResponse(
        List<Item> items
) {
    public record Item(
            UUID id,
            String skuCode,
            String variantName,
            Integer variantPrice,
            String status
    ) {}
}
