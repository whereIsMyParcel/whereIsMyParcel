package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

	@Override
	public UserClient create(Throwable cause) {
		log.warn("[CircuitBreaker] user-service 호출 실패", cause);
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
