package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.ShortestPathResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "hub-service", path = "/internal/v1", fallbackFactory = HubClientFallbackFactory.class)
public interface HubClient {

    @GetMapping("/hubs/{hubId}/exists")
    ApiResponse<Boolean> exists(@PathVariable UUID hubId);

    @GetMapping("/hub-routes/shortest-path")
    ApiResponse<ShortestPathResponse> getShortestPath(
            @RequestParam UUID originHubId,
            @RequestParam UUID destinationHubId
    );
}
