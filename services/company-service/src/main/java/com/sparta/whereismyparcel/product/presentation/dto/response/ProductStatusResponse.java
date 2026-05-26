package com.sparta.whereismyparcel.product.presentation.dto.response;

import com.sparta.whereismyparcel.product.domain.entity.Product;
import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;

import java.util.UUID;

public record ProductStatusResponse(
        UUID productId,
        UUID companyId,
        UUID hubId,
        ProductStatus productStatus
) {
    public static ProductStatusResponse from(Product product) {
        return new ProductStatusResponse(
                product.getId(),
                product.getCompanyId(),
                product.getHubId(),
                product.getStatus()
        );
    }
}
