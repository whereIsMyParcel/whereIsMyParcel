package com.sparta.whereismyparcel.order.presentation.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record OrderUpdateRequest(
        @Size(max = 500)
        String requestMemo,

        @FutureOrPresent
        LocalDateTime requestedDeliveryAt
) {
}
