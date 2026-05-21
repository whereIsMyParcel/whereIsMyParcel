package com.sparta.whereismyparcel.shipment.presentation.dto.request;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryManagerCreateRequest(
        @Schema(description = "허브 id(허브 담당 배송자의 경우 null)")
        UUID hubId,

        @Schema(description = "배송담당자의 slack id")
        @NotBlank
        String slackId,

        @Schema(description = "배송 담당자 타입")
        @NotNull
        DeliveryType type
) {
}
