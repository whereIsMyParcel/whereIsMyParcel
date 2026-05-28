package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.ShipmentResponse;
import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;

import com.sparta.whereismyparcel.common.response.ApiResponse;
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
		log.warn("[CircuitBreaker] shipment-service 호출 실패", cause);
		return (userId, orderId) -> {
			throw new ServiceUnavailableException();
		};
	}
}
