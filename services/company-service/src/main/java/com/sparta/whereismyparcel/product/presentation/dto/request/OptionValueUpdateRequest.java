package com.sparta.whereismyparcel.product.presentation.dto.request;

import java.util.UUID;

public record OptionValueUpdateRequest(
        String value,
        Integer additionalPrice
) {
}
