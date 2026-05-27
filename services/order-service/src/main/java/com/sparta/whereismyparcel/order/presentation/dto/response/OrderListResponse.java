package com.sparta.whereismyparcel.order.presentation.dto.response;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderListResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus orderStatus,
        Long totalPrice,
        String recipientName,
        LocalDateTime requestedDeliveryAt,
        LocalDateTime orderedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderListResponse from(Order order) {
        return new OrderListResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getTotalPrice(),
                order.getRecipientName(),
                order.getRequestedDeliveryAt(),
                order.getOrderedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
