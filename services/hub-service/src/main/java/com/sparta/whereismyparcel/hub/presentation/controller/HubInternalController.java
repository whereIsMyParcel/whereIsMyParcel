package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.hub.application.service.HubService;
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
 * 타 마이크로서비스(Order, Company, Delivery 등)와의 내부 통신을 전담하는 Controller.
 * 외부(클라이언트) 노출을 막기 위해 Gateway에서 JWT 필터를 타지 않도록 구성
 */
public class HubInternalController {

    private final HubService hubService;
    private final ShortestPathService shortestPathService;

    /**
     * 허브 존재 여부 검증 API
     * From: Company Service (업체 등록 시 허브 ID 유효성 체크)
     */
    @GetMapping("/hubs/{hubId}/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateHub(@PathVariable UUID hubId) {
        try {
            hubService.getHub(hubId);
            return ResponseEntity.ok(ApiResponse.success(true));
        } catch (HubNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    /**
     * 다익스트라 최단 경로 조회 API
     * From: Order / Shipment Service (배송 경로 생성 시 사용)
     */
    @GetMapping("/hub-routes/shortest-path")
    public ResponseEntity<ApiResponse<ShortestPathResponse>> getShortestPath(
            @RequestParam UUID originHubId,
            @RequestParam UUID destinationHubId) {
        return ResponseEntity.ok(ApiResponse.success(
                shortestPathService.getShortestPath(originHubId, destinationHubId)));
    }
}
