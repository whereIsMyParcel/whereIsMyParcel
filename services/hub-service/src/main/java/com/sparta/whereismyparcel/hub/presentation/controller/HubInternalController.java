package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.hub.application.service.HubQueryService;
import com.sparta.whereismyparcel.hub.application.service.ShortestPathService;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubResponse;
import com.sparta.whereismyparcel.hub.presentation.dto.response.ShortestPathResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
/**
 * 타 마이크로서비스(Order, Company, Delivery 등)와의 통신을 전담하는 Controller.
 * 외부(클라이언트) 호출을 막기 위해 Gateway에서 JWT 필터를 타지 않도록 구성
 */
public class HubInternalController {

    private final HubQueryService hubQueryService;
    private final ShortestPathService shortestPathService;

    /**
     * 허브 존재 여부 확인 API
     * 타 서비스 통일 스펙: GET /internal/v1/hubs/{hubId}/exists
     */
    @GetMapping("/hubs/{hubId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkHubExists(@PathVariable UUID hubId) {
        try {
            hubQueryService.getHub(hubId);
            return ResponseEntity.ok(ApiResponse.success(true));
        } catch (HubNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    /**
     * 허브 간 최단 경로 조회 API
     * 타 서비스 통일 스펙: GET /internal/v1/hub-routes/shortest-path
     */
    @GetMapping("/hub-routes/shortest-path")
    public ResponseEntity<ApiResponse<ShortestPathResponse>> getShortestPath(
            @RequestParam UUID originHubId,
            @RequestParam UUID destinationHubId) {
        return ResponseEntity.ok(ApiResponse.success(
                shortestPathService.getShortestPath(originHubId, destinationHubId)));
    }
}
