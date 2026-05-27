package com.sparta.whereismyparcel.order.presentation.dto.response;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderDispatchDeadlineUpdateResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus orderStatus,
        LocalDateTime requestedDeliveryAt,
        LocalDateTime finalDispatchDeadline
) {
    public static OrderDispatchDeadlineUpdateResponse from(Order order) {
        return new OrderDispatchDeadlineUpdateResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getRequestedDeliveryAt(),
                order.getFinalDispatchDeadline()
        );
    }
}
