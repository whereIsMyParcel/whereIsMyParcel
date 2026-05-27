package com.sparta.whereismyparcel.shipment.presentation.dto.request;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;

import java.util.UUID;

public record DeliveryManagerSearchRequest(
        UUID hubId,
        DeliveryType type,
        String slackId
) {}