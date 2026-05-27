package com.sparta.whereismyparcel.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateRequest(

        @Schema(description = "상품명", example = "아이폰 17 Pro")
        @NotNull
        String name,

        @Schema(description = "상품 설명")
        @NotNull
        String description,

        @Schema(description = "상품 기본 가격", example = "1250000")
        @NotNull
        Integer price
) {
}
