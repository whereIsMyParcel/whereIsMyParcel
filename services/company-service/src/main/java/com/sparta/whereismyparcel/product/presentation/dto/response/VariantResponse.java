package com.sparta.whereismyparcel.product.presentation.dto.response;

import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;
import com.sparta.whereismyparcel.product.domain.entity.ProductVariant;

import java.util.UUID;

public record VariantResponse(
        UUID variantId,
        String skuCode,
        String variantName,
        Integer variantPrice,
        ProductStatus status
) {
    public static VariantResponse from(ProductVariant variant) {
        return new VariantResponse(
                variant.getId(),
                variant.getSkuCode(),
                variant.getVariantName(),
                variant.getVariantPrice(),
                variant.getStatus()
        );
    }
}
