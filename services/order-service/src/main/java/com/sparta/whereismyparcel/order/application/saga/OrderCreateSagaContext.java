package com.sparta.whereismyparcel.order.application.saga;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class OrderCreateSagaContext {

    private UUID orderId;
    private final String userId;
    private final UUID companyMemberId;
    private final String orderNumber;
    private final String recipientName;
    private final String recipientPhone;
    private final String zipCode;
    private final String address;
    private final String addressDetail;
    private final String requestMemo;
    private final LocalDateTime requestedDeliveryAt;
    private final List<OrderItemInfo> items;

    private List<StockReservation> reservations = new ArrayList<>();
    private List<UUID> shipmentIds = new ArrayList<>();

    public OrderCreateSagaContext(
            String userId,
            UUID companyMemberId,
            String orderNumber,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address,
            String addressDetail,
            String requestMemo,
            LocalDateTime requestedDeliveryAt,
            List<OrderItemInfo> items
    ) {
        this.userId = userId;
        this.companyMemberId = companyMemberId;
        this.orderNumber = orderNumber;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.requestMemo = requestMemo;
        this.requestedDeliveryAt = requestedDeliveryAt;
        this.items = items;
    }

    public void applyOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public void applyReservation(List<StockReservation> reservations) {
        this.reservations = reservations;
    }

    public void applyShipmentIds(List<UUID> shipmentIds) {
        this.shipmentIds = shipmentIds;
    }

    public boolean isStockReserved() {
        return !reservations.isEmpty();
    }

    public record OrderItemInfo(
            UUID productVariantId,
            String skuCode,
            String variantName,
            Long unitPrice,
            Integer quantity
    ) {}

    public record StockReservation(UUID variantId, String skuCode, Integer quantity) {}
}
