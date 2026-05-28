package com.sparta.whereismyparcel.company.infrastructure.feign.client;

import com.sparta.whereismyparcel.company.infrastructure.feign.client.HubFeignClientFallbackFactory;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "hub-service", fallbackFactory = HubFeignClientFallbackFactory.class)
public interface HubFeignClient {

    // 업체, 재고 허브 존재 검증 요청
    @GetMapping("/internal/v1/hubs/{hubId}/exists")
    ApiResponse<Boolean> isHubExists(@PathVariable UUID hubId);
}
