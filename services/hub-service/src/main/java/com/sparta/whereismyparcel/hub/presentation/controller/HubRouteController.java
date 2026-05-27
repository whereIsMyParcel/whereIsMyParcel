package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.common.security.UserRole;
import com.sparta.whereismyparcel.hub.application.service.HubRouteCommandService;
import com.sparta.whereismyparcel.hub.application.service.HubRouteQueryService;
import com.sparta.whereismyparcel.hub.domain.exception.ForbiddenException;
import com.sparta.whereismyparcel.hub.presentation.controller.util.PaginationController;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hub-routes")
@RequiredArgsConstructor
public class HubRouteController {

    private final HubRouteCommandService hubRouteCommandService;
    private final HubRouteQueryService hubRouteQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<HubRouteResponse>> createHubRoute(
            @RequestHeader("X-User-Role") String role,
            @RequestBody @Valid CreateHubRouteRequest request) {
        validateAdminRole(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(hubRouteCommandService.createHubRoute(request)));
    }

    @GetMapping("/{hubRouteId}")
    public ResponseEntity<ApiResponse<HubRouteResponse>> getHubRoute(@PathVariable UUID hubRouteId) {
        return ResponseEntity.ok(ApiResponse.success(hubRouteQueryService.getHubRoute(hubRouteId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HubRouteResponse>>> getHubRoutes(
            @PageableDefault(size = 10) Pageable pageable) {
        PaginationController.validatePageSize(pageable);
        return ResponseEntity.ok(ApiResponse.success(hubRouteQueryService.getHubRoutes(pageable)));
    }

    @PatchMapping("/{hubRouteId}")
    public ResponseEntity<ApiResponse<HubRouteResponse>> updateHubRoute(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID hubRouteId,
            @RequestBody @Valid UpdateHubRouteRequest request) {
        validateAdminRole(role);
        return ResponseEntity.ok(ApiResponse.success(hubRouteCommandService.updateHubRoute(hubRouteId, request)));
    }

    @DeleteMapping("/{hubRouteId}")
    public ResponseEntity<ApiResponse<Void>> deleteHubRoute(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID hubRouteId) {
        validateAdminRole(role);
        hubRouteCommandService.deleteHubRoute(hubRouteId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private void validateAdminRole(String role) {
        if (!UserRole.MASTER.getRoleName().equals(role) && !UserRole.HUB_MANAGER.getRoleName().equals(role)) {
            throw new ForbiddenException();
        }
    }
}
