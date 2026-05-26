package com.sparta.whereismyparcel.order.domain.entity;

import com.sparta.whereismyparcel.common.entity.BaseEntity;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderItemsException;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderStatusException;
import com.sparta.whereismyparcel.order.domain.exception.OrderCancelTimeExpiredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "company_member_id", nullable = false, length = 100)
    private UUID companyMemberId;

    @Column(name = "order_number", nullable = false, unique = true, length = 100)
    private String orderNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "recipient_name", nullable = false, length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 50)
    private String recipientPhone;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "request_memo", length = 500)
    private String requestMemo;

    @Column(name = "delivery_deadline")
    private LocalDateTime deliveryDeadline;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    @Column(name = "ordered_by", length = 100)
    private String orderedBy;

    @Builder(access = AccessLevel.PRIVATE)
    private Order(
            UUID companyMemberId,
            String orderNumber,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address,
            String addressDetail,
            String requestMemo,
            LocalDateTime deliveryDeadline,
            String orderedBy
    ) {
        this.companyMemberId = companyMemberId;
        this.orderNumber = orderNumber;
        this.orderStatus = OrderStatus.PENDING;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.requestMemo = requestMemo;
        this.deliveryDeadline = deliveryDeadline;
        this.orderedAt = LocalDateTime.now();
        this.orderedBy = orderedBy;
    }

    public static Order create(
            UUID companyMemberId,
            String orderNumber,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address,
            String addressDetail,
            String requestMemo,
            LocalDateTime deliveryDeadline,
            String orderedBy,
            List<OrderItem> orderItems
    ) {
        validateOrderItems(orderItems);

        Order order = Order.builder()
                .companyMemberId(companyMemberId)
                .orderNumber(orderNumber)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .zipCode(zipCode)
                .address(address)
                .addressDetail(addressDetail)
                .requestMemo(requestMemo)
                .deliveryDeadline(deliveryDeadline)
                .orderedBy(orderedBy)
                .build();

        orderItems.forEach(order::addItem);
        order.totalPrice = order.calculateTotalPrice();

        return order;
    }

    public void reserveStock() {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException();
        }
        this.orderStatus = OrderStatus.STOCK_RESERVED;
    }

    public void confirm() {
        if (this.orderStatus != OrderStatus.STOCK_RESERVED) {
            throw new InvalidOrderStatusException();
        }
        this.orderStatus = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (
                this.orderStatus != OrderStatus.PENDING
                && this.orderStatus != OrderStatus.STOCK_RESERVED
                && this.orderStatus != OrderStatus.CONFIRMED
        ) {
            throw new InvalidOrderStatusException();
        }
        this.orderStatus = OrderStatus.CANCELLED;
    }

    public void fail() {
        if (
                this.orderStatus != OrderStatus.PENDING
                        && this.orderStatus != OrderStatus.STOCK_RESERVED
        ) {
            throw new InvalidOrderStatusException();
        }
        this.orderStatus = OrderStatus.FAILED;
    }

    public void complete() {
        if (this.orderStatus != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStatusException();
        }
        this.orderStatus = OrderStatus.COMPLETED;
    }

    public void delete(String userId) {
        softDelete(userId);
        this.orderItems.forEach(item -> item.softDelete(userId));
    }

    public void updateRequestInfo(String requestMemo, LocalDateTime deliveryDeadline) {
        if (
                this.orderStatus != OrderStatus.PENDING
                        && this.orderStatus != OrderStatus.STOCK_RESERVED
        ) {
            throw new InvalidOrderStatusException();
        }

        this.requestMemo = requestMemo;
        this.deliveryDeadline = deliveryDeadline;
    }

    public boolean isDeletable() {
        return this.orderStatus == OrderStatus.CANCELLED
                || this.orderStatus == OrderStatus.COMPLETED
                || this.orderStatus == OrderStatus.FAILED;
    }

    private void addItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    public void validateCancelableTime(LocalDateTime now, Duration cancelLimit) {
        if (orderedAt.plus(cancelLimit).isBefore(now)) {
            throw new OrderCancelTimeExpiredException();
        }
    }

    private static void validateOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new InvalidOrderItemsException();
        }
    }

    private Long calculateTotalPrice() {
        return this.orderItems.stream()
                .mapToLong(OrderItem::calculateTotalPrice)
                .sum();
    }
}
