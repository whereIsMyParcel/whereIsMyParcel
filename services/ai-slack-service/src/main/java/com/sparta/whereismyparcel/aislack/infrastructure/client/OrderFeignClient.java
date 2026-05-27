package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.DeliveryDeadlinePatchRequest;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/internal/v1/orders/{orderId}")
    ApiResponse<OrderResponse> getOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID orderId
    );

    @PatchMapping("/internal/v1/orders/{orderId}/delivery-deadline")
    ApiResponse<Void> patchDeliveryDeadline(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID orderId,
            @RequestBody DeliveryDeadlinePatchRequest request
    );
}
