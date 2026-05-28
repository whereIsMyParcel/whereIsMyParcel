package com.sparta.whereismyparcel.shipment.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.application.service.ShipmentService;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.ShipmentSearchRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.ShipmentViewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "shipments", description = "배송 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shipments")
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
            summary = "배송 삭제",
            description = "해당 배송을 삭제 처리한다"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    @DeleteMapping("/{shipmentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID shipmentId
    ) {
        shipmentService.delete(userId, shipmentId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(
            summary = "배송 시작",
            description = "배송을 시작 처리하고 재고를 차감한다"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER')")
    @PostMapping("/{shipmentId}/start")
    public ResponseEntity<ApiResponse<Void>> start(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID shipmentId
    ) {
        shipmentService.start(userId, shipmentId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(
            summary = "배송 1건 조회",
            description = "배송 ID를 기준으로 배송 상세 정보를 조회한다"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER', 'COMPANY_MANAGER')")
    @GetMapping("/{shipmentId}")
    public ResponseEntity<ApiResponse<ShipmentViewResponse>> getShipment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID shipmentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.getShipment(userId, shipmentId)));
    }

    @Operation(
            summary = "배송 목록 조회",
            description = "조건에 따라 배송 목록을 페이징 조회한다"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ShipmentViewResponse>>> search(
            @ModelAttribute ShipmentSearchRequest request,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(shipmentService.search(request, pageable)));
    }
}
