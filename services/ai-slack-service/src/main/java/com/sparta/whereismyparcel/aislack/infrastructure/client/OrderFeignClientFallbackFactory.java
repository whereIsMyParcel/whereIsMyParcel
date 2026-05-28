package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.DeliveryDeadlinePatchRequest;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.OrderResponse;
import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OrderFeignClientFallbackFactory implements FallbackFactory<OrderFeignClient> {

	@Override
	public OrderFeignClient create(Throwable cause) {
		log.warn("[CircuitBreaker] order-service 호출 실패", cause);
		return new OrderFeignClient() {
			@Override
			public ApiResponse<OrderResponse> getOrder(String userId, UUID orderId) {
				throw new ServiceUnavailableException();
			}

			@Override
			public ApiResponse<Void> patchDeliveryDeadline(String userId, UUID orderId, DeliveryDeadlinePatchRequest request) {
				throw new ServiceUnavailableException();
			}
		};
	}
}
