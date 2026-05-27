package com.sparta.whereismyparcel.order.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockReservationRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.SkuValidationResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.StockReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyFeignClient {

    @GetMapping("/internal/v1/products/valid")
    ApiResponse<SkuValidationResponse> validateProducts(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam List<UUID> productVariantIds
    );

    @PostMapping("/internal/v1/inventories/reserve")
    ApiResponse<List<StockReservationResponse>> reserveStock(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody StockReservationRequest request
    );

    @PostMapping("/internal/v1/inventories/cancel")
    ApiResponse<Void> cancelReservation(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody StockCancelRequest request
    );
}
