package com.sparta.whereismyparcel.product.presentation.dto.response;

import com.sparta.whereismyparcel.product.domain.entity.Product;

import java.util.UUID;

public record ProductUpdateResponse(
        UUID productId,
        String name,
        UUID companyId,
        UUID hubId,
        String description,
        Integer price
) {
    public static  ProductUpdateResponse from (Product product) {
        return new ProductUpdateResponse(
                product.getId(),
                product.getName(),
                product.getCompanyId(),
                product.getHubId(),
                product.getDescription(),
                product.getPrice()
        );
    }
}
