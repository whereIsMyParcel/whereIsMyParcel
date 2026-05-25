package com.sparta.whereismyparcel.shipment.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ShipmentCancelRequest(
        @Schema(description = "주문 id")
        @NotNull
        UUID orderId
) {
}