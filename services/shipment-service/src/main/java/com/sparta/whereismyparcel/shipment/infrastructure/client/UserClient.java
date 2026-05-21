package com.sparta.whereismyparcel.shipment.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/internal/v1/users")
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/{slackId}/exists")
    boolean exists(@PathVariable String slackId);
}
