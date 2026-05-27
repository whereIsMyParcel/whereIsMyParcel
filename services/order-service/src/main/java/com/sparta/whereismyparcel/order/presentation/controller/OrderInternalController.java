package com.sparta.whereismyparcel.order.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.service.OrderService;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderDispatchDeadlineUpdateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderAiContextResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCompleteResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderDispatchDeadlineUpdateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PatchMapping("/{orderId}/deliverydeadline")
    public ResponseEntity<ApiResponse<OrderDispatchDeadlineUpdateResponse>> updateFinalDispatchDeadline(
            @PathVariable UUID orderId,
            @RequestBody @Valid OrderDispatchDeadlineUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateFinalDispatchDeadline(orderId, request)
        ));
    }

    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<OrderCompleteResponse>> completeOrder(
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(ApiResponse.success(orderService.completeOrder(orderId)));
    }
}
