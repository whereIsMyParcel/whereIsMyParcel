package com.sparta.whereismyparcel.order.application.service;

import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.repository.OrderRepository;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;

    @Transactional
    public OrderCreateResponse createOrder(String userId, OrderCreateRequest request) {
        List<OrderItem> orderItems = request.items().stream()
                .map(this::createOrderItemAssumingProductLookupSucceeded)
                .toList();

        Order order = Order.create(
                request.companyMemberId(),
                generateOrderNumber(),
                request.recipientName(),
                request.recipientPhone(),
                request.zipCode(),
                request.address(),
                request.addressDetail(),
                request.requestMemo(),
                request.deliveryDeadline(),
                userId,
                orderItems
        );

        orderRepository.save(order);
        return OrderCreateResponse.from(order);
    }

    // TODO: Feign 호출 없이 구현하기 위한 임시 메서드(주문 상품들 검증했다고 치고...)
    private OrderItem createOrderItemAssumingProductLookupSucceeded(
            OrderCreateRequest.OrderItemCreateRequest item
    ) {
        return OrderItem.create(
                item.productVariantId(),
                "TEMP_PRODUCT_NAME",
                "TEMP_OPTION",
                1_000L,
                item.quantity()
        );
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(ORDER_NUMBER_DATE_FORMAT);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return "ORD-" + date + "-" + suffix;
    }
}
