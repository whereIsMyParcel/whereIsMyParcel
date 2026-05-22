package com.sparta.whereismyparcel.order.infrastructure.client;

import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.ShipmentCreateRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.ShipmentStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "shipment-service")
public interface ShipmentFeignClient {
    @PostMapping("/internal/v1/shipments")
    List<UUID> createShipments(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ShipmentCreateRequest request
    );

    @GetMapping("/internal/v1/shipments")
    List<ShipmentStatusResponse> getShipmentsByOrderId(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam UUID orderId
    );

    @PostMapping("/internal/v1/shipments/cancel")
    void cancelShipments(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam UUID orderId
    );
}
