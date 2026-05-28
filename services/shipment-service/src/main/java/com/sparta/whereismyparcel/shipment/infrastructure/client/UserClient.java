package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/internal/v1/users", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/by-slack/{slackId}")
    ApiResponse<Boolean> exists(@PathVariable String slackId);

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") UUID userId);
}
