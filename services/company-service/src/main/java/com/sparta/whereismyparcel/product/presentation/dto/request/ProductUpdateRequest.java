package com.sparta.whereismyparcel.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateRequest(

        @Schema(name = "상품명", example = "아이폰 17 Pro")
        @NotNull
        String name,

        @Schema(name = "상품 설명")
        @NotNull
        String description,

        @Schema(name = "상품 기본 가격", example = "1250000")
        @NotNull
        Integer price
) {
}
