package com.sparta.whereismyparcel.order.infrastructure.client.dto.response;

import java.util.UUID;

public record ShipmentStatusResponse(
        UUID shipmentId,
        String status
) {}