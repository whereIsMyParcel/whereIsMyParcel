package com.sparta.whereismyparcel.product.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OptionRegisterRequest(

        @Schema(description = "옵션 명", example = "색상")
        @NotBlank
        String name,

        @Valid
        @NotNull
        List<OptionValueRegisterRequest> optionValue
) {
}
