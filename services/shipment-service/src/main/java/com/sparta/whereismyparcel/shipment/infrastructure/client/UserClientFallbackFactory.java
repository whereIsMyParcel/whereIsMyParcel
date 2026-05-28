package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.UserResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

	@Override
	public UserClient create(Throwable cause) {
		if (cause instanceof CallNotPermittedException) {
			log.warn("[CircuitBreaker] user-service 호출 차단 (Circuit Open)");
		} else {
			log.warn("[CircuitBreaker] user-service 호출 실패", cause);
		}
		return new UserClient() {
			@Override
			public ApiResponse<Boolean> exists(String slackId) {
				throw new ServiceUnavailableException();
			}

			@Override
			public ApiResponse<UserResponse> getUser(UUID userId) {
				throw new ServiceUnavailableException();
			}
		};
	}
}
