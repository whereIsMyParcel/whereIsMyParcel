package com.sparta.whereismyparcel.order.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.service.OrderService;
import com.sparta.whereismyparcel.order.domain.OrderStatus;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderDispatchDeadlineUpdateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderAiContextResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderDispatchDeadlineUpdateResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderIdsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderAiContextResponse>> getOrderAiContext(
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderAiContext(orderId)));
    }

    // logistics-agent scheduled scan(design §16.2): 상태별 고장 후보 orderId 열거.
    @GetMapping
    public ResponseEntity<ApiResponse<OrderIdsResponse>> getOrderIdsByStatus(
            @RequestParam OrderStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.findOrderIdsByStatus(status)));
    }

    @PatchMapping("/{orderId}/finalDispatchDeadline")
    public ResponseEntity<ApiResponse<OrderDispatchDeadlineUpdateResponse>> updateFinalDispatchDeadline(
            @PathVariable UUID orderId,
            @RequestBody @Valid OrderDispatchDeadlineUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateFinalDispatchDeadline(orderId, request)
        ));
    }

    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeOrder(
            @PathVariable UUID orderId
    ) {
        orderService.completeOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
