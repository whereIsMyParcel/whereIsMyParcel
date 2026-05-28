package com.sparta.whereismyparcel.order.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockReservationRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.SkuValidationResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.StockReservationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CompanyFeignClientFallbackFactory implements FallbackFactory<CompanyFeignClient> {

    @Override
    public CompanyFeignClient create(Throwable cause) {
        log.warn("[CircuitBreaker] company-service 호출 실패", cause);
        return new CompanyFeignClient() {
            @Override
            public ApiResponse<List<SkuValidationResponse>> validateProducts(String userId, List<UUID> productVariantIds) {
                throw new ServiceUnavailableException();
            }

            @Override
            public ApiResponse<List<StockReservationResponse>> reserveStock(String userId, StockReservationRequest request) {
                throw new ServiceUnavailableException();
            }

            @Override
            public ApiResponse<Void> cancelReservation(String userId, StockCancelRequest request) {
                throw new ServiceUnavailableException();
            }
        };
    }
}
