package com.sparta.whereismyparcel.shipment.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.application.service.DeliveryManagerService;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DeliveryManagerCreateRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.DeliveryManagerCreateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "delivery-manager", description = "배송담당자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/delivery-managers")
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    @Operation(summary = "배송담당자 등록", description = "배송 담당자를 등록한다")
    @PreAuthorize("hasAnyRole('MASTER', 'HUB_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryManagerCreateResponse>> create(@Valid @RequestBody DeliveryManagerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(deliveryManagerService.create(request)));
    }
}
