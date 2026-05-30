package com.sparta.whereismyparcel.order.application.service;

import com.sparta.whereismyparcel.order.application.saga.OrderCreateSagaContext;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.exception.OrderNotFoundException;
import com.sparta.whereismyparcel.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreationStateService {

    private final OrderRepository orderRepository;

    @Transactional
    public UUID createPendingOrder(OrderCreateSagaContext context) {
        List<OrderItem> orderItems = context.getItems().stream()
                .map(item -> OrderItem.create(
                        item.productVariantId(),
                        item.skuCode(),
                        item.variantName(),
                        item.variantName(),
                        item.unitPrice(),
                        item.quantity()
                ))
                .toList();

        Order order = Order.create(
                context.getCompanyMemberId(),
                context.getOrderNumber(),
                context.getRecipientName(),
                context.getRecipientPhone(),
                context.getZipCode(),
                context.getAddress(),
                context.getAddressDetail(),
                context.getRequestMemo(),
                context.getRequestedDeliveryAt(),
                context.getUserId(),
                orderItems
        );

        orderRepository.save(order);
        log.info("[OrderCreationStateService] PENDING 주문 저장. orderId={}", order.getOrderId());
        return order.getOrderId();
    }

    @Transactional
    public void markStockReserved(UUID orderId) {
        Order order = findOrder(orderId);
        order.reserveStock();
        log.info("[OrderCreationStateService] STOCK_RESERVED 반영. orderId={}", orderId);
    }

    @Transactional
    public void markConfirmed(UUID orderId) {
        Order order = findOrder(orderId);
        order.confirm();
        log.info("[OrderCreationStateService] CONFIRMED 반영. orderId={}", orderId);
    }

    @Transactional
    public void markFailed(UUID orderId) {
        Order order = findOrder(orderId);
        order.fail();
        log.info("[OrderCreationStateService] FAILED 반영. orderId={}", orderId);
    }

    @Transactional
    public void markCompensationFailed(UUID orderId) {
        Order order = findOrder(orderId);
        order.failCompensation();
        log.info("[OrderCreationStateService] COMPENSATION_FAILED 반영. orderId={}", orderId);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return findOrder(orderId);
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
    }
}
