package com.sparta.whereismyparcel.product.presentation.dto.request;

import com.sparta.whereismyparcel.product.domain.entity.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OptionValueStatusRequest(

        @Schema(example = "ACTIVE,INACTIVE")
        @NotNull
        ProductStatus status
) {
}
