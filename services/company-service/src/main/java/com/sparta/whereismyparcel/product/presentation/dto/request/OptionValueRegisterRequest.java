package com.sparta.whereismyparcel.product.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record OptionValueRegisterRequest(
        @NotBlank
        String value,

        @NotNull
        @PositiveOrZero
        Integer additionalPrice
) {
}
