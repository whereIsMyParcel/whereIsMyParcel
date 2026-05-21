package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;

import java.util.UUID;

public record DeliveryManagerCreateResponse(
        UUID id,
        UUID hubId,
        String slackId,
        String type,
        int deliveryOrder
) {
    public static DeliveryManagerCreateResponse from(DeliveryManager entity) {
        return new DeliveryManagerCreateResponse(
                entity.getId(),
                entity.getHubId(),
                entity.getSlackId(),
                entity.getType().name(),
                entity.getDeliveryOrder()
        );
    }
}
