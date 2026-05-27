package com.sparta.whereismyparcel.shipment.presentation.dto.request;

import com.sparta.whereismyparcel.shipment.domain.entity.ShipmentStatus;

import java.util.UUID;

public record ShipmentSearchRequest(
        UUID orderId,
        UUID originHubId,
        UUID currentHubId,
        UUID destinationHubId,
        UUID companyDeliveryManagerId,
        String shipmentNumber,
        ShipmentStatus shipmentStatus,
        String deliveryAddress,
        String recipientName,
        String recipientSlackId
) {}