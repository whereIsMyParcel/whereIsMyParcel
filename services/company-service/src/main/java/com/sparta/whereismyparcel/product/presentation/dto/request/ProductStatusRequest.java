package com.sparta.whereismyparcel.product.presentation.dto.request;

import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
        @NotNull
        ProductStatus status
) {
}
