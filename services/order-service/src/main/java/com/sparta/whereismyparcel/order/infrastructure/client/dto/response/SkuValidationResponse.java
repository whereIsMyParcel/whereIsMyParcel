package com.sparta.whereismyparcel.order.infrastructure.client.dto.response;

import java.util.UUID;

public record SkuValidationResponse(
        UUID variantId,
        String skuCode,
        String variantName,
        Integer variantPrice,
        String status
) {}
