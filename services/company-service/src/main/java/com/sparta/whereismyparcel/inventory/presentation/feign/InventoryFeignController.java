package com.sparta.whereismyparcel.inventory.presentation.feign;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.inventory.application.service.InventoryService;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockConfirmRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.StockReservationRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.StockReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/inventories")
public class InventoryFeignController {

    private final InventoryService inventoryService;

    /**
     * 주문 생성 시 수량 예약
     */
    @PostMapping
    public ApiResponse<List<StockReservationResponse>> reserveStock(
            @RequestBody StockReservationRequest request) {

        List<StockReservationResponse> response = inventoryService.reserveOrderStock(request);
        return ApiResponse.success(response);
    }

    /**
     * 주문 취소 시 재고 복구
     */
    @PostMapping("/cancel")
    public ApiResponse<Void> cancelReservation(@RequestBody StockCancelRequest request) {
        inventoryService.cancelOrderReservation(request);
        return ApiResponse.success(null);
    }

    /**
     * 배송 시작 시 출고 확정
     */
    @PostMapping("/confirm")
    public void confirmDeliveryLaunch(@RequestBody StockConfirmRequest request) {
        inventoryService.confirmDeliveryLaunch(request);
    }
}
