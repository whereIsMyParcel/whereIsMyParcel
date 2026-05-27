package com.sparta.whereismyparcel.shipment.presentation.dto.request;

import com.sparta.whereismyparcel.shipment.domain.entity.ShipmentItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;

public record DecreaseInventoryRequest(
        Map<UUID, Integer> productQuantities
) {

    public static DecreaseInventoryRequest from(
            List<ShipmentItem> shipmentItems
    ) {
        Map<UUID, Integer> productQuantities = shipmentItems.stream()
                .collect(toMap(
                        ShipmentItem::getOrderItemId,
                        ShipmentItem::getQuantity,
                        Integer::sum
                ));

        return new DecreaseInventoryRequest(productQuantities);
    }
}