package com.sparta.whereismyparcel.product.presentation.dto.request;

import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;

import java.util.UUID;

public record OptionValueStatusRequest(
    ProductStatus status
) {
}
