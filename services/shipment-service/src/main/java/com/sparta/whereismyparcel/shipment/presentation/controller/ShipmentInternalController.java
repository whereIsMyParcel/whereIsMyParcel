package com.sparta.whereismyparcel.shipment.presentation.controller;



import com.sparta.whereismyparcel.common.response.ApiResponse;

import com.sparta.whereismyparcel.shipment.application.service.ShipmentService;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.ShipmentCancelRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "shipment-internal", description = "배송 내부 API")
@RestController
@RequiredArgsConstructor
public class ShipmentInternalController {

    private final ShipmentService shipmentService;

    @Operation(
            summary = "주문에 속한 배송 전체 취소",
            description = "주문에 속한 모든 배송이 출발 전 상태일 경우 배송을 취소한다"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER')")
    @PostMapping("/internal/v1/shipments/cancel")
    public ApiResponse<Void> cancelShipments(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ShipmentCancelRequest request
    ) {
        shipmentService.cancel(userId,request.orderId());
        return ApiResponse.ok();
    }
}
