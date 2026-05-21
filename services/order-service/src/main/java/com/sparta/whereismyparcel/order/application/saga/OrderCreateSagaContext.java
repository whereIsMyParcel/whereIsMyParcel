package com.sparta.whereismyparcel.order.application.saga;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class OrderCreateSagaContext {

    private final UUID orderId;
    private final String userId;
    private final List<OrderItemInfo> items;

    private List<StockReservation> reservations = new ArrayList<>();
    private List<UUID> shipmentIds = new ArrayList<>();

    public OrderCreateSagaContext(UUID orderId, String userId, List<OrderItemInfo> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
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

    public record OrderItemInfo(UUID productVariantId, Integer quantity) {}
    public record StockReservation(UUID skuId, UUID hubId, Integer quantity) {}
}
