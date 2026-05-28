package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.UserResponse;
import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

	@Override
	public UserFeignClient create(Throwable cause) {
		log.warn("[CircuitBreaker] user-service 호출 실패", cause);
		return userId -> {
			throw new ServiceUnavailableException();
		};
	}
}
