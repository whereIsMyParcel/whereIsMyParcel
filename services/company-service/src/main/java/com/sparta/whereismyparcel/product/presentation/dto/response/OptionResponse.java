package com.sparta.whereismyparcel.product.presentation.dto.response;

import com.sparta.whereismyparcel.product.domain.entity.ProductOption;

import java.util.List;
import java.util.UUID;

public record OptionResponse(
        UUID optionId,
        String name,
        List<OptionValueResponse> optionValues
) {
    public static OptionResponse from(ProductOption option, List<OptionValueResponse> optionValues) {
        return new OptionResponse(
                option.getId(),
                option.getName(),
                optionValues
        );
    }
}
