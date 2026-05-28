package com.sparta.whereismyparcel.shipment.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.application.service.DeliveryManagerService;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerCreateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerSearchRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerUpdateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.DeliveryManagerCreateResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.DeliveryManagerViewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "delivery-manager", description = "배송담당자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery-managers")
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    @Operation(summary = "배송담당자 등록", description = "배송 담당자를 등록한다")
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryManagerCreateResponse>> create(@Valid @RequestBody DeliveryManagerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(deliveryManagerService.create(request)));
    }

    @Operation(
            summary = "배송 담당자 수정",
            description = "배송 담당자의 허브, 슬랙ID, 타입을 수정한다 (배송 순서는 수정 불가)"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    @PatchMapping("/{deliveryManagerId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID deliveryManagerId,
            @RequestBody DeliveryManagerUpdateRequest request
    ) {
        deliveryManagerService.update(userId, deliveryManagerId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(
            summary = "배송 담당자 삭제",
            description = "배송 담당자를 삭제 처리한다 (soft delete)"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    @DeleteMapping("/{deliveryManagerId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID deliveryManagerId
    ) {
        deliveryManagerService.delete(userId, deliveryManagerId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(
            summary = "배송 담당자 단 건 조회",
            description = "배송 담당자 ID로 단건 조회한다"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER')")
    @GetMapping("/{deliveryManagerId}")
    public ResponseEntity<ApiResponse<DeliveryManagerViewResponse>> getDeliveryManager(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID deliveryManagerId
    ) {
        return ResponseEntity.ok(ApiResponse.success(deliveryManagerService.getDeliveryManager(userId, deliveryManagerId)));
    }

    @Operation(
            summary = "배송 담당자 검색",
            description = "조건 기반으로 배송 담당자를 조회한다"
    )
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeliveryManagerViewResponse>>> search(
            @RequestHeader("X-User-Id") String userId,
            DeliveryManagerSearchRequest request,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(deliveryManagerService.search(userId, request, pageable))
        );
    }
}
