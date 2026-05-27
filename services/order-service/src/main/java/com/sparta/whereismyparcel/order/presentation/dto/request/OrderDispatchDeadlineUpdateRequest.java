package com.sparta.whereismyparcel.order.presentation.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record OrderDispatchDeadlineUpdateRequest(
        @NotNull
        @FutureOrPresent
        LocalDateTime finalDispatchDeadline
) {
}
