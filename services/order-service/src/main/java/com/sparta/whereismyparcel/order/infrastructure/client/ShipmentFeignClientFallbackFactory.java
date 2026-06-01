package com.sparta.whereismyparcel.order.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.ShipmentCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.ShipmentCreateRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.ShipmentCreateResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.ShipmentStatusResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ShipmentFeignClientFallbackFactory implements FallbackFactory<ShipmentFeignClient> {

    @Override
    public ShipmentFeignClient create(Throwable cause) {
        if (cause instanceof CallNotPermittedException) {
            log.warn("[CircuitBreaker] shipment-service 호출 차단 (Circuit Open)");
        } else {
            log.warn("[CircuitBreaker] shipment-service 호출 실패", cause);
        }
        return new ShipmentFeignClient() {
            @Override
            public ApiResponse<List<ShipmentCreateResponse>> createShipments(String userId, ShipmentCreateRequest request) {
                throw new ServiceUnavailableException();
            }

            @Override
            public ApiResponse<List<ShipmentStatusResponse>> getShipmentsByOrderId(String userId, UUID orderId) {
                throw new ServiceUnavailableException();
            }

            @Override
            public ApiResponse<Void> cancelShipments(String userId, ShipmentCancelRequest request) {
                throw new ServiceUnavailableException();
            }
        };
    }
}
