package com.sparta.whereismyparcel.hub.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateHubRouteRequest(
    @NotNull(message = "거리는 필수입니다.")
    @Positive(message = "거리는 0보다 커야 합니다.")
    Double distance,

    @NotNull(message = "소요 시간은 필수입니다.")
    @Positive(message = "소요 시간은 0보다 커야 합니다.")
    Integer duration
) {}
