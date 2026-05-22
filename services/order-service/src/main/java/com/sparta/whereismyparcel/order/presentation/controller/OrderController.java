package com.sparta.whereismyparcel.order.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.service.OrderService;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid OrderCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(orderService.createOrder(userId, request)));
    }
}
