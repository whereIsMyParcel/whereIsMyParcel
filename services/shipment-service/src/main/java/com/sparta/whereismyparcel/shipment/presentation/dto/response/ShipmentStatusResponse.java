package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import java.util.UUID;

public record ShipmentStatusResponse(
        UUID shipmentId,
        String status
) {}