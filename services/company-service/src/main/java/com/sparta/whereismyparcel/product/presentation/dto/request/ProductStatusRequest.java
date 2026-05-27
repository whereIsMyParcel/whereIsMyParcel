package com.sparta.whereismyparcel.product.presentation.dto.request;

import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(

        @Schema(description = "상품 상태", example = "ACTIVE,INACTIVE")
        @NotNull
        ProductStatus status
) {
}