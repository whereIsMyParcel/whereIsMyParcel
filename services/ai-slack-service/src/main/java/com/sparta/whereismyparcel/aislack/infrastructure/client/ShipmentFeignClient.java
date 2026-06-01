package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.ShipmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable; // @PathVariable 임포트 추가

import java.util.List;
import java.util.UUID;

@FeignClient(name = "shipment-service", fallbackFactory = ShipmentFeignClientFallbackFactory.class)
public interface ShipmentFeignClient {

    @GetMapping("/internal/v1/shipments/{orderId}")
    ApiResponse<List<ShipmentResponse>> getShipmentByOrderId(
            @PathVariable UUID orderId // @PathVariable로 변경
    );

}
