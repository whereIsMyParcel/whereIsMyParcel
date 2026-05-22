package com.sparta.whereismyparcel.order.infrastructure.client.dto.request;

import java.util.UUID;

public record ShipmentCancelRequest(
        UUID orderId
) {
}
