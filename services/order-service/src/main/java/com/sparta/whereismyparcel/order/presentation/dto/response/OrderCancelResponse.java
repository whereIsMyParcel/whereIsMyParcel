package com.sparta.whereismyparcel.order.presentation.dto.response;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;

import java.util.UUID;

public record OrderCancelResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus orderStatus
) {
    public static OrderCancelResponse from(Order order) {
        return new OrderCancelResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus()
        );
    }
}
