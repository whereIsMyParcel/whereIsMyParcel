package com.sparta.whereismyparcel.shipment.presentation.dto.request;

public record GetDestinationHubIdRequest(
        String zipCode,
        String address,
        String addressDetails
) {
}
