package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/internal/v1/users")
public interface UserClient {

    @GetMapping("/{slackId}/exists")
    ApiResponse<Boolean> exists(@PathVariable String slackId);
}
