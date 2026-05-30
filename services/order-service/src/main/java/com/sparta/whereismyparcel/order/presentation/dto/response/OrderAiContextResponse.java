package com.sparta.whereismyparcel.order.presentation.dto.response;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderAiContextResponse(
        UUID orderId,
        String orderNumber,
        String recipientName,
        String recipientAddress,
        LocalDateTime requestedDeliveryAt,
        OrderStatus orderStatus,
        String requestMemo,
        LocalDateTime orderedAt,
        List<ItemResponse> items
) {
    public static OrderAiContextResponse from(Order order) {
        return new OrderAiContextResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getRecipientName(),
                buildRecipientAddress(order),
                order.getRequestedDeliveryAt(),
                order.getOrderStatus(),
                order.getRequestMemo(),
                order.getOrderedAt(),
                order.getOrderItems().stream()
                        .map(ItemResponse::from)
                        .toList()
        );
    }

    private static String buildRecipientAddress(Order order) {
        if (order.getAddressDetail() == null || order.getAddressDetail().isBlank()) {
            return order.getAddress();
        }
        return order.getAddress() + " " + order.getAddressDetail();
    }

    public record ItemResponse(
            UUID productVariantId,
            String productNameSnapshot,
            String productOptionSnapshot,
            Long unitPrice,
            Integer quantity,
            Long totalPrice
    ) {
        private static ItemResponse from(OrderItem orderItem) {
            return new ItemResponse(
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
