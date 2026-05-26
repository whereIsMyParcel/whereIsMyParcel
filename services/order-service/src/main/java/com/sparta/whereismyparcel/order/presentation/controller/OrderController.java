package com.sparta.whereismyparcel.order.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.common.util.PageableUtils;
import com.sparta.whereismyparcel.order.application.service.OrderService;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCancelResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCreateResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderDetailResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid OrderCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(orderService.createOrder(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderListResponse>>> getOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrders(
                userId,
                role,
                status,
                keyword,
                startDate,
                endDate,
                PageableUtils.normalize(pageable, ALLOWED_SORT_FIELDS, DEFAULT_SORT)
        )));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(userId, role, orderId)));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderCancelResponse>> cancelOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(userId, orderId)));
    }
}
