package com.sparta.whereismyparcel.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OptionValueUpdateRequest(

        @Schema(description = "옵션 값", example = "블랙")
        @NotNull
        String value,

        @Schema(description = "추가 금액", example = "0")
        Integer additionalPrice
) {
}
