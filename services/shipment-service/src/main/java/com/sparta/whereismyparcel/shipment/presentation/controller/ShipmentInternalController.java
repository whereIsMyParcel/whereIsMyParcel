package com.sparta.whereismyparcel.shipment.presentation.controller;


import com.sparta.whereismyparcel.common.response.ApiResponse;

import com.sparta.whereismyparcel.shipment.application.service.ShipmentService;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.ShipmentCancelRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.ShipmentCreateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.ShipmentCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "shipment-internal", description = "배송 내부 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/shipments")
public class ShipmentInternalController {

    private final ShipmentService shipmentService;

    @Operation(
            summary = "주문에 속한 배송 전체 취소",
            description = "주문에 속한 모든 배송이 출발 전 상태일 경우 배송을 취소한다"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER')")
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelShipments(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ShipmentCancelRequest request
    ) {
        shipmentService.cancel(userId, request.orderId());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(
            summary = "배송 생성",
            description = "주문 정보를 기반으로 배송 정보와 배송 경로를 생성 및 저장한다"
    )
    @PreAuthorize("hasAnyRole('MASTER')")
    @PostMapping()
    public ResponseEntity<ApiResponse<List<ShipmentCreateResponse>>> create(@RequestHeader("X-User-Id") String userId,
                                                                            @RequestBody ShipmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(shipmentService.create(userId, request)));
    }
}
