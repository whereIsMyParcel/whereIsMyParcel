package com.sparta.whereismyparcel.order.presentation.dto.response;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID orderId,
        UUID companyMemberId,
        String orderNumber,
        OrderStatus orderStatus,
        Long totalPrice,
        String recipientName,
        String recipientPhone,
        String zipCode,
        String address,
        String addressDetail,
        String requestMemo,
        LocalDateTime requestedDeliveryAt,
        LocalDateTime finalDispatchDeadline,
        LocalDateTime orderedAt,
        String orderedBy,
        List<OrderItemResponse> items
) {
    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getOrderId(),
                order.getCompanyMemberId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getTotalPrice(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getZipCode(),
                order.getAddress(),
                order.getAddressDetail(),
                order.getRequestMemo(),
                order.getRequestedDeliveryAt(),
                order.getFinalDispatchDeadline(),
                order.getOrderedAt(),
                order.getOrderedBy(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList()
        );
    }

    public record OrderItemResponse(
            UUID orderItemId,
            UUID productVariantId,
            String productNameSnapshot,
            String productOptionSnapshot,
            Long unitPrice,
            Integer quantity,
            Long totalPrice
    ) {
        private static OrderItemResponse from(OrderItem orderItem) {
            return new OrderItemResponse(
                    orderItem.getOrderItemId(),
                    orderItem.getProductVariantId(),
                    orderItem.getProductNameSnapshot(),
                    orderItem.getProductOptionSnapshot(),
                    orderItem.getUnitPrice(),
                    orderItem.getQuantity(),
                    orderItem.calculateTotalPrice()
            );
        }
    }
}
