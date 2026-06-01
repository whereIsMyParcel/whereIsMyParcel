package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.UserResponse;
import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

	@Override
	public UserFeignClient create(Throwable cause) {
		if (cause instanceof CallNotPermittedException) {
			log.warn("[CircuitBreaker] user-service 호출 차단 (Circuit Open)");
		} else {
			log.warn("[CircuitBreaker] user-service 호출 실패", cause);
		}
		return new UserFeignClient() {
			@Override
			public ApiResponse<UserResponse> getUser(UUID userId) {
				throw new ServiceUnavailableException();
			}
		};
	}
}
