package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.exception.ServiceUnavailableException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DecreaseInventoryRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.GetDestinationHubIdRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.GetDestinationHubIdResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.GetProductHubIdResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CompanyClientFallbackFactory implements FallbackFactory<CompanyClient> {

	@Override
	public CompanyClient create(Throwable cause) {
		if (cause instanceof CallNotPermittedException) {
			log.warn("[CircuitBreaker] company-service 호출 차단 (Circuit Open)");
		} else {
			log.warn("[CircuitBreaker] company-service 호출 실패", cause);
		}
		return new CompanyClient() {
			@Override
			public ApiResponse<GetDestinationHubIdResponse> getDestinationHubId(GetDestinationHubIdRequest request) {
				throw new ServiceUnavailableException();
			}

			@Override
			public ApiResponse<Void> decrease(DecreaseInventoryRequest request) {
				throw new ServiceUnavailableException();
			}

			@Override
			public ApiResponse<List<GetProductHubIdResponse>> getHubMappingsByProductIds(List<UUID> productVariantIds) {
				throw new ServiceUnavailableException();
			}
		};
	}
}
