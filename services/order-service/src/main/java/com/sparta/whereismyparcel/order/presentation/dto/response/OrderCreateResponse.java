package com.sparta.whereismyparcel.order.presentation.dto.response;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreateResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus orderStatus,
        Long totalPrice,
        LocalDateTime requestedDeliveryAt,
        LocalDateTime finalDispatchDeadline,
        LocalDateTime orderedAt
) {
    public static OrderCreateResponse from(Order order) {
        return new OrderCreateResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getTotalPrice(),
                order.getRequestedDeliveryAt(),
                order.getFinalDispatchDeadline(),
                order.getOrderedAt()
        );
    }
}
