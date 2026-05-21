package com.sparta.whereismyparcel.hub.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

public record CreateHubRequest(
    @NotBlank(message = "허브 이름은 필수입니다.")
    String name,

    @NotBlank(message = "허브 주소는 필수입니다.")
    String address,

    @NotNull(message = "위도는 필수입니다.")
    @Range(min = -90, max = 90, message = "위도는 -90에서 90 사이여야 합니다.")
    Double latitude,

    @NotNull(message = "경도는 필수입니다.")
    @Range(min = -180, max = 180, message = "경도는 -180에서 180 사이여야 합니다.")
    Double longitude
) {}
