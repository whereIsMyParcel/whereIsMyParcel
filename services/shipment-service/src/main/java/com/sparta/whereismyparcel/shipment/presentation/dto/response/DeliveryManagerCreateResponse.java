package com.sparta.whereismyparcel.shipment.presentation.dto.response;

import com.sparta.whereismyparcel.shipment.domain.entity.DeliveryManager;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record DeliveryManagerCreateResponse(
        @Schema(description = "배송 담당자 id")
        UUID id,
        @Schema(description = "허브 id(허브 담당 배송자의 경우 null)")
        UUID hubId,
        @Schema(description = "배송담당자의 slack id")
        String slackId,
        @Schema(description = "배송 담당자 타입")
        String type,
        @Schema(description = "배송 담당자 순번")
        int deliveryOrder
) {
    public static DeliveryManagerCreateResponse from(DeliveryManager entity) {
        return new DeliveryManagerCreateResponse(
                entity.getId(),
                entity.getHubId(),
                entity.getSlackId(),
                entity.getType().name(),
                entity.getDeliveryOrder()
        );
    }
}
