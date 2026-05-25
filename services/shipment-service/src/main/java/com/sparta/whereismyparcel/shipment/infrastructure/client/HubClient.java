package com.sparta.whereismyparcel.shipment.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping("/internal/v1/hubs")
@FeignClient(name = "hub-service")
public interface HubClient {

    @GetMapping("/{hubId}/exists")
    boolean exists(@PathVariable UUID hubId);
}
