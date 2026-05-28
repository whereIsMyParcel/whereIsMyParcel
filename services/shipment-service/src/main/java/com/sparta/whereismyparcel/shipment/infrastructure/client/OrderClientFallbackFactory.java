package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {

	@Override
	public OrderClient create(Throwable cause) {
		if (cause instanceof CallNotPermittedException) {
			log.warn("[CircuitBreaker] order-service 호출 차단 (Circuit Open)");
		} else {
			log.warn("[CircuitBreaker] order-service 호출 실패", cause);
		}
		return new OrderClient() {
			@Override
			public ApiResponse<Void> complete(UUID orderId) {
				throw new ServiceUnavailableException();
			}
		};
	}
}
