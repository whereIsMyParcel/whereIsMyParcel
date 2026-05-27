package com.sparta.whereismyparcel.order.presentation.dto.response;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderUpdateResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus orderStatus,
        String requestMemo,
        LocalDateTime requestedDeliveryAt
) {
    public static OrderUpdateResponse from(Order order) {
        return new OrderUpdateResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getRequestMemo(),
                order.getRequestedDeliveryAt()
        );
    }
}
