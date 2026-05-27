package com.sparta.whereismyparcel.product.presentation.dto.response;

import com.sparta.whereismyparcel.product.domain.entity.Product;
import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;

import java.util.UUID;

public record ProductPageResponse(
        UUID productId,
        String name,
        UUID companyId,
        UUID hubId,
        Integer price,
        ProductStatus status
) {
    public static ProductPageResponse from(Product product) {
        return new ProductPageResponse(
                product.getId(),
                product.getName(),
                product.getCompanyId(),
                product.getHubId(),
                product.getPrice(),
                product.getStatus()
        );
    }
}
