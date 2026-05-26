package com.sparta.whereismyparcel.shipment.presentation.dto.request;

import java.util.Map;
import java.util.UUID;

public record DecreaseInventoryRequest(
        Map<UUID, Integer> productQuantities
) {
}