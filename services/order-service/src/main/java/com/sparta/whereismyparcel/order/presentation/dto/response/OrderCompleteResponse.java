package com.sparta.whereismyparcel.order.presentation.dto.response;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;

import java.util.UUID;

public record OrderCompleteResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus orderStatus
) {
    public static OrderCompleteResponse from(Order order) {
        return new OrderCompleteResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus()
        );
    }
}
