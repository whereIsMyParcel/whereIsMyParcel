package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;

import java.util.UUID;

public record DeliveryManagerViewResponse(
        UUID deliveryManagerId,
        UUID hubId,
        String slackId,
        DeliveryType type,
        int deliveryOrder
) {

    public static DeliveryManagerViewResponse from(DeliveryManager deliveryManager) {
        return new DeliveryManagerViewResponse(
                deliveryManager.getId(),
                deliveryManager.getHubId(),
                deliveryManager.getSlackId(),
                deliveryManager.getType(),
                deliveryManager.getDeliveryOrder()
        );
    }
}