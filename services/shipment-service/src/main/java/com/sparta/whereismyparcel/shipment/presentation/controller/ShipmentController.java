package com.sparta.whereismyparcel.shipment.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.application.service.ShipmentService;
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
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER')")
    @PatchMapping("/{shipmentId}/delivered")
    public ResponseEntity<ApiResponse<Void>> delivered(@RequestHeader("X-User-Id") String userId,
                                                       @PathVariable UUID shipmentId) {
        shipmentService.delivered(userId, shipmentId);
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
