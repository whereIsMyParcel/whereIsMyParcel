package com.sparta.whereismyparcel.common.infrastructure.client;

import com.sparta.whereismyparcel.common.dto.HubValidateRequest;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "Hub-Service")
public interface HubFeignClient {

    // 업체, 상품, 재고 허브 존재 검증 요청
    @GetMapping("internal/v1/hubs")
    ApiResponse<Void> validateHub(
            @RequestBody HubValidateRequest request
    );
}
