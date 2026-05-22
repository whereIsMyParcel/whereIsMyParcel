package com.sparta.whereismyparcel.hub.presentation.dto.response;

import java.util.List;
import java.util.UUID;

public record ShortestPathResponse(
    Double totalDistance,
    Integer totalDuration,
    List<RouteSegmentResponse> routes
) {
    public record RouteSegmentResponse(
        Integer sequence,
        UUID originHubId,
        String originHubName,
        UUID destinationHubId,
        String destinationHubName,
        Double estimatedDistance,
        Integer estimatedDuration
    ) {}
}
