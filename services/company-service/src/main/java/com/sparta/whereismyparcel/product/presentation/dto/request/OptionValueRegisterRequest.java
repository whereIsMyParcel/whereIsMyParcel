package com.sparta.whereismyparcel.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record OptionValueRegisterRequest(

        @Schema(description = "옵션 값", example = "화이트")
        @NotBlank
        String value,

        @Schema(description = "추가 금액", example = "0")
        @NotNull
        @PositiveOrZero
        Integer additionalPrice
) {
}
