package com.sparta.whereismyparcel.product.presentation.dto.response;

import com.sparta.whereismyparcel.product.domain.entity.Product;
import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID productId,
        String name,
        UUID companyId,
        UUID hubId,
        String description,
        Integer price,
        ProductStatus status,
        List<OptionResponse> options,
        List<VariantResponse> variants
) {
    public static ProductResponse from(Product product, List<OptionResponse> options, List<VariantResponse> variants) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCompanyId(),
                product.getHubId(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                options,
                variants
        );
    }
}
