package com.sparta.whereismyparcel.shipment.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.application.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "shipments", description = "배송 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @Operation(
            summary = "배송 완료 처리",
            description = "업체 이동 중인 배송을 배송 완료 상태로 변경한다"
    )
    @PatchMapping("/{shipmentId}/delivered")
    public ApiResponse<Void> delivered(@RequestHeader("X-User-Id") String userId,
                                       @PathVariable UUID shipmentId) {
        shipmentService.delivered(userId, shipmentId);
        return ApiResponse.ok();
    }
}
