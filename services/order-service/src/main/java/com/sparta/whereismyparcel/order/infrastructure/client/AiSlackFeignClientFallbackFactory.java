package com.sparta.whereismyparcel.order.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.AiAnalysisRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class AiSlackFeignClientFallbackFactory implements FallbackFactory<AiSlackFeignClient> {

	@Override
	public AiSlackFeignClient create(Throwable cause) {
		if (cause instanceof CallNotPermittedException) {
			log.warn("[CircuitBreaker] ai-slack-service 호출 차단 (Circuit Open)");
		} else {
			log.warn("[CircuitBreaker] ai-slack-service 호출 실패", cause);
		}
		return new AiSlackFeignClient() {
			@Override
			public ApiResponse<UUID> createAiAnalysisRequest(String userId, AiAnalysisRequest request) {
				throw new ServiceUnavailableException();
			}
		};
	}
}
