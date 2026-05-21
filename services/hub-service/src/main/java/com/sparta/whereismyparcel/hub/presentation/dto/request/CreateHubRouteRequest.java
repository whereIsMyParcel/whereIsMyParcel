package com.sparta.whereismyparcel.hub.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateHubRouteRequest(
    @NotNull(message = "출발 허브 ID는 필수입니다.")
    UUID originHubId,

    @NotNull(message = "목적 허브 ID는 필수입니다.")
    UUID destinationHubId,

    @NotNull(message = "거리는 필수입니다.")
    @Positive(message = "거리는 0보다 커야 합니다.")
    Double distance,

    @NotNull(message = "소요 시간은 필수입니다.")
    @Positive(message = "소요 시간은 0보다 커야 합니다.")
    Integer duration
) {}
