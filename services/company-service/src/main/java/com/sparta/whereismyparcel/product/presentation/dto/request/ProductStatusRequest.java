package com.sparta.whereismyparcel.product.presentation.dto.request;

import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;

public record ProductStatusRequest(
        ProductStatus status
) {
}
