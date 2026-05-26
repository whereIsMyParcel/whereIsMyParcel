package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@FeignClient(name = "hub-service", path = "/internal/v1/hubs")
public interface HubClient {

    @GetMapping("/{hubId}/exists")
    ApiResponse<Boolean> exists(@PathVariable UUID hubId);
}
