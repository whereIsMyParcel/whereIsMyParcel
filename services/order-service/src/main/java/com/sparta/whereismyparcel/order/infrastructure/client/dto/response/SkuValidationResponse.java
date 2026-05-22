package com.sparta.whereismyparcel.order.infrastructure.client.dto.response;

import java.util.List;
import java.util.UUID;

public record SkuValidationResponse(
        List<Item> items
) {
    public record Item(
            UUID skuId,
            String productName,
            String optionName,
            Long unitPrice
    ) {}
}