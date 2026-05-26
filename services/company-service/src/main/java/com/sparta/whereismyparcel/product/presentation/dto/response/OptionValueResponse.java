package com.sparta.whereismyparcel.product.presentation.dto.response;

import com.sparta.whereismyparcel.product.domain.entity.ProductOptionValue;

import java.util.UUID;

public record OptionValueResponse(
        UUID id,
        String value,
        Integer additionalPrice
) {
    public static OptionValueResponse from(ProductOptionValue optionValue) {
        return new OptionValueResponse(
                optionValue.getId(),
                optionValue.getValue(),
                optionValue.getAdditionalPrice());
    }
}
