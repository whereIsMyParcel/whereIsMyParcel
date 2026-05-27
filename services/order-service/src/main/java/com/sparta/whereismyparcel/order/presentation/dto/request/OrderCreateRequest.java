package com.sparta.whereismyparcel.order.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull
        UUID companyMemberId,

        @Size(max = 500)
        String requestMemo,

        @FutureOrPresent
        LocalDateTime requestedDeliveryAt,

        @NotBlank
        @Size(max = 50)
        String recipientName,

        @NotBlank
        @Size(max = 30)
        String recipientPhone,

        @NotBlank
        @Size(max = 20)
        String zipCode,

        @NotBlank
        @Size(max = 255)
        String address,

        @Size(max = 255)
        String addressDetail,

        @NotEmpty
        @Valid
        List<OrderItemCreateRequest> items
) {
    public record OrderItemCreateRequest(
            @NotNull
            UUID productVariantId,

            @NotNull
            @Positive
            Integer quantity
    ) {
    }
}
