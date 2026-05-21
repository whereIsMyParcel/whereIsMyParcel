package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.hub.application.service.HubRouteService;
import com.sparta.whereismyparcel.hub.domain.exception.ForbiddenException;
import com.sparta.whereismyparcel.hub.domain.exception.InvalidPageSizeException;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRouteRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.request.UpdateHubRouteRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubRouteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hub-routes")
@RequiredArgsConstructor
/**
 * 허브 간 이동 경로와 예상 소요 시간 정보를 관리하는 Controller.
 * 생성/수정/삭제 시 MASTER, HUB_MANAGER 권한 검증이 필수입니다.
 */
public class HubRouteController {

    private final HubRouteService hubRouteService;

    @PostMapping
    public ResponseEntity<ApiResponse<HubRouteResponse>> createHubRoute(
            @RequestHeader("X-User-Role") String role,
            @RequestBody @Valid CreateHubRouteRequest request) {
        validateAdminRole(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(hubRouteService.createHubRoute(request)));
    }

    @GetMapping("/{hubRouteId}")
    public ResponseEntity<ApiResponse<HubRouteResponse>> getHubRoute(@PathVariable UUID hubRouteId) {
        return ResponseEntity.ok(ApiResponse.success(hubRouteService.getHubRoute(hubRouteId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HubRouteResponse>>> getHubRoutes(
            @PageableDefault(size = 10) Pageable pageable) {
        validatePageSize(pageable.getPageSize());
        return ResponseEntity.ok(ApiResponse.success(hubRouteService.getHubRoutes(pageable)));
    }

    @PatchMapping("/{hubRouteId}")
    public ResponseEntity<ApiResponse<HubRouteResponse>> updateHubRoute(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID hubRouteId,
            @RequestBody @Valid UpdateHubRouteRequest request) {
        validateAdminRole(role);
        return ResponseEntity.ok(ApiResponse.success(hubRouteService.updateHubRoute(hubRouteId, request)));
    }

    @DeleteMapping("/{hubRouteId}")
    public ResponseEntity<ApiResponse<Void>> deleteHubRoute(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID hubRouteId) {
        validateAdminRole(role);
        hubRouteService.deleteHubRoute(hubRouteId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private void validateAdminRole(String role) {
        if (!"MASTER".equals(role) && !"HUB_MANAGER".equals(role)) {
            throw new ForbiddenException();
        }
    }

    private void validatePageSize(int size) {
        if (!List.of(10, 30, 50).contains(size)) {
            throw new InvalidPageSizeException();
        }
    }
}
