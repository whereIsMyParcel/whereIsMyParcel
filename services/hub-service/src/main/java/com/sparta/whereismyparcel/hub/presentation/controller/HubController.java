package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.common.security.UserRole;
import com.sparta.whereismyparcel.hub.application.service.HubCommandService;
import com.sparta.whereismyparcel.hub.application.service.HubQueryService;
import com.sparta.whereismyparcel.hub.domain.exception.ForbiddenException;
import com.sparta.whereismyparcel.hub.presentation.controller.util.PaginationController;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.request.UpdateHubRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubResponse;
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
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubCommandService hubCommandService;
    private final HubQueryService hubQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<HubResponse>> createHub(
            @RequestHeader("X-User-Role") String role,
            @RequestBody @Valid CreateHubRequest request) {
        validateAdminRole(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(hubCommandService.createHub(
                        request.name(), request.address(), request.latitude(), request.longitude())));
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> getHub(@PathVariable UUID hubId) {
        return ResponseEntity.ok(ApiResponse.success(hubQueryService.getHub(hubId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HubResponse>>> getHubs(
            @PageableDefault(size = 10) Pageable pageable) {
        PaginationController.validatePageSize(pageable);
        return ResponseEntity.ok(ApiResponse.success(hubQueryService.getHubs(pageable)));
    }

    @PatchMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> updateHub(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID hubId,
            @RequestBody @Valid UpdateHubRequest request) {
        validateAdminRole(role);
        return ResponseEntity.ok(ApiResponse.success(hubCommandService.updateHub(
                hubId, request.name(), request.address(), request.latitude(), request.longitude())));
    }

    @DeleteMapping("/{hubId}")
    public ResponseEntity<ApiResponse<Void>> deleteHub(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID hubId) {
        validateAdminRole(role);
        hubCommandService.deleteHub(hubId, userId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private void validateAdminRole(String role) {
        if (!UserRole.MASTER.getRoleName().equals(role) && !UserRole.HUB_MANAGER.getRoleName().equals(role)) {
            throw new ForbiddenException();
        }
    }
}
