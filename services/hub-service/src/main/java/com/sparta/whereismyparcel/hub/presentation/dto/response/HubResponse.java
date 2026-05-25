package com.sparta.whereismyparcel.hub.presentation.dto.response;

import com.sparta.whereismyparcel.hub.domain.entity.Hub;

import java.util.UUID;

public record HubResponse(
    UUID hubId,
    String name,
    String address,
    Double latitude,
    Double longitude
) {
    public static HubResponse from(Hub hub) {
        return new HubResponse(
            hub.getHubId(),
            hub.getName(),
            hub.getAddress(),
            hub.getLatitude(),
            hub.getLongitude()
        );
    }
}
