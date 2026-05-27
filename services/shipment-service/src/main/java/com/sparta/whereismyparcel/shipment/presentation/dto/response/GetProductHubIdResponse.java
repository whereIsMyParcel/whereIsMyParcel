package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import org.hibernate.validator.cfg.defs.UUIDDef;

import java.util.UUID;

public record GetProductHubIdResponse(
        UUID variantId,
        UUID hubId
) {
}
