package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.hub.application.service.HubService;
import com.sparta.whereismyparcel.hub.domain.exception.ForbiddenException;
import com.sparta.whereismyparcel.hub.domain.exception.InvalidPageSizeException;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
/**
 * 17개 물류 허브의 CRUD를 담당하는 Controller.
 * 외부 API이며, 생성/수정/삭제는 권한(MASTER/HUB_MANAGER) 검증이 필수입니다.
 */
public class HubController {

    private final HubService hubService;

    @PostMapping
    public ResponseEntity<ApiResponse<HubResponse>> createHub(
            @RequestHeader("X-User-Role") String role,
            @RequestBody @Valid CreateHubRequest request) {
        validateAdminRole(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(hubService.createHub(request)));
    }

    @GetMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> getHub(@PathVariable UUID hubId) {
        return ResponseEntity.ok(ApiResponse.success(hubService.getHub(hubId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HubResponse>>> getHubs(
            @PageableDefault(size = 10) Pageable pageable) {
        validatePageSize(pageable.getPageSize());
        return ResponseEntity.ok(ApiResponse.success(hubService.getHubs(pageable)));
    }

    @PatchMapping("/{hubId}")
    public ResponseEntity<ApiResponse<HubResponse>> updateHub(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID hubId,
            @RequestBody @Valid UpdateHubRequest request) {
        validateAdminRole(role);
        return ResponseEntity.ok(ApiResponse.success(hubService.updateHub(hubId, request)));
    }

    @DeleteMapping("/{hubId}")
    public ResponseEntity<ApiResponse<Void>> deleteHub(
            @RequestHeader("X-User-Role") String role,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID hubId) {
        validateAdminRole(role);
        hubService.deleteHub(hubId, userId);
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
