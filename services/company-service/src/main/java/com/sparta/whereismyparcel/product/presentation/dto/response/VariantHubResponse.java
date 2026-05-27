package com.sparta.whereismyparcel.product.presentation.dto.response;

import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;

import java.util.UUID;

public record VariantHubResponse(
        UUID variantId,
        UUID hubId
) {
    public static VariantHubResponse from(ProductVariant variant) {
        return new VariantHubResponse(
                variant.getId(),
                variant.getProduct().getHubId()
        );
    }
}
