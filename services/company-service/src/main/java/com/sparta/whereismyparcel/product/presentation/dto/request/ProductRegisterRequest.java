package com.sparta.whereismyparcel.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record ProductRegisterRequest(

        @Schema(name = "상품 명", example = "아이폰 17 Pro")
        @NotBlank
        String name,

        @NotNull
        UUID companyId,

        @NotNull
        UUID hubId,

        @Schema(name = "상품 설명")
        String description,

        @Schema(name = "상품 기본 가격", example = "1250000")
        @NotNull
        @PositiveOrZero
        Integer price,

        @Valid
        @NotNull
        List<OptionRegisterRequest> options


) {
}
