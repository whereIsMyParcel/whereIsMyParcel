package com.sparta.whereismyparcel.product.presentation.dto.request;

public record ProductUpdateRequest(
        String name,
        String description,
        Integer price
) {
}
