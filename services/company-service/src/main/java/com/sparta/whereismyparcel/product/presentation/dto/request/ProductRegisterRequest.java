package com.sparta.whereismyparcel.product.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record ProductRegisterRequest(
        @NotBlank
        String name,

        @NotNull
        UUID companyId,

        @NotNull
        UUID hubId,

        String description,

        @NotNull
        @PositiveOrZero
        Integer price,

        @Valid
        @NotNull
        List<OptionRegisterRequest> options


) {
}
