package com.sparta.whereismyparcel.inventory.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.inventory.application.service.InventoryService;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockConfirmRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockReservationRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.StockReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/inventories")
public class InventoryInternalController {

    private final InventoryService inventoryService;

    /**
     * 주문 생성 시 수량 예약
     */
    @PostMapping("/reserve")
    @Retryable(
            retryFor = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    public ResponseEntity<ApiResponse<List<StockReservationResponse>>> reserveStock(
            @RequestBody StockReservationRequest request) {

        List<StockReservationResponse> response = inventoryService.reserveOrderStock(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 주문 취소 시 재고 복구
     */
    @PostMapping("/cancel")
    @Retryable(
            retryFor = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    public ResponseEntity<ApiResponse<Void>> cancelReservation(@RequestBody StockCancelRequest request) {
        inventoryService.cancelOrderReservation(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 배송 시작 시 출고 확정
     */
    @PostMapping("/confirm")
    @Retryable(
            retryFor = {PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    public ResponseEntity<ApiResponse<Void>> confirmDeliveryLaunch(@RequestBody StockConfirmRequest request) {
        inventoryService.confirmDeliveryLaunch(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
