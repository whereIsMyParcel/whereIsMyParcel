package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service", path = "/internal/v1/orders")
public interface OrderClient {

    @PatchMapping("/{orderId}/complete")
    ApiResponse<Void> complete(@PathVariable UUID orderId);
}
