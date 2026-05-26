package com.sparta.whereismyparcel.product.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OptionRegisterRequest(
        @NotBlank
        String name,

        @Valid
        @NotNull
        List<OptionValueRegisterRequest> optionValue
) {
}
