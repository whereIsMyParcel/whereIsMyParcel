package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {

	@Override
	public OrderClient create(Throwable cause) {
		log.warn("[CircuitBreaker] order-service 호출 실패", cause);
		return orderId -> {
			throw new ServiceUnavailableException();
		};
	}
}
