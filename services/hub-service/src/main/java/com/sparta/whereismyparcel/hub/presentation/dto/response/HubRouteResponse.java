package com.sparta.whereismyparcel.hub.presentation.dto.response;

import com.sparta.whereismyparcel.hub.domain.entity.HubRoute;

import java.util.UUID;

public record HubRouteResponse(
    UUID hubRouteId,
    UUID originHubId,
    String originHubName,
    UUID destinationHubId,
    String destinationHubName,
    Double distance,
    Integer duration
) {
    public static HubRouteResponse from(HubRoute route) {
        return new HubRouteResponse(
            route.getHubRouteId(),
            route.getOriginHub().getHubId(),
            route.getOriginHub().getName(),
            route.getDestinationHub().getHubId(),
            route.getDestinationHub().getName(),
            route.getDistance(),
            route.getDuration()
        );
    }
}
