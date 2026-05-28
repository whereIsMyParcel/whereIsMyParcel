package com.sparta.whereismyparcel.aislack.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", fallbackFactory = UserFeignClientFallbackFactory.class)
public interface UserFeignClient {

    @GetMapping("/internal/v1/users/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable UUID userId);
}
