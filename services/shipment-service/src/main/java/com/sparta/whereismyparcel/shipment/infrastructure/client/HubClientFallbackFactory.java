package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.ShortestPathResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class HubClientFallbackFactory implements FallbackFactory<HubClient> {

	@Override
	public HubClient create(Throwable cause) {
		if (cause instanceof CallNotPermittedException) {
			log.warn("[CircuitBreaker] hub-service 호출 차단 (Circuit Open)");
		} else {
			log.warn("[CircuitBreaker] hub-service 호출 실패", cause);
		}
		return new HubClient() {
			@Override
			public ApiResponse<Boolean> exists(UUID hubId) {
				throw new ServiceUnavailableException();
			}

			@Override
			public ApiResponse<ShortestPathResponse> getShortestPath(UUID originHubId, UUID destinationHubId) {
				throw new ServiceUnavailableException();
			}
		};
	}
}
