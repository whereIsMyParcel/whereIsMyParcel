package com.sparta.whereismyparcel.order.domain.entity;

import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderItemsException;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderStatusException;
import com.sparta.whereismyparcel.order.domain.exception.OrderCancelTimeExpiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    @DisplayName("주문 생성 시 최초 상태는 PENDING이다")
    void createOrderWithPendingStatus() {
        // given
        List<OrderItem> orderItems = List.of(createOrderItem(10_000L, 2));

        // when
        Order order = createOrder(orderItems);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 생성 시 주문상품 합계로 총액을 계산한다")
    void createOrderWithCalculatedTotalPrice() {
        // given
        List<OrderItem> orderItems = List.of(
                createOrderItem(10_000L, 2),
                createOrderItem(15_000L, 3)
        );

        // when
        Order order = createOrder(orderItems);

        // then
        assertThat(order.getTotalPrice()).isEqualTo(65_000L);
    }

    @Test
    @DisplayName("주문상품이 null이면 주문을 생성할 수 없다")
    void createOrderWithNullOrderItemsThrowsException() {
        assertThatThrownBy(() -> createOrder(null))
                .isInstanceOf(InvalidOrderItemsException.class);
    }

    @Test
    @DisplayName("주문상품이 비어 있으면 주문을 생성할 수 없다")
    void createOrderWithEmptyOrderItemsThrowsException() {
        assertThatThrownBy(() -> createOrder(List.of()))
                .isInstanceOf(InvalidOrderItemsException.class);
    }

    @Test
    @DisplayName("PENDING 상태 주문은 STOCK_RESERVED로 변경할 수 있다")
    void reserveStockPendingOrder() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));

        // when
        order.reserveStock();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
    }

    @Test
    @DisplayName("STOCK_RESERVED 상태 주문은 CONFIRMED로 변경할 수 있다")
    void confirmStockReservedOrder() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.reserveStock();

        // when
        order.confirm();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("PENDING 상태 주문은 CANCELLED로 변경할 수 있다")
    void cancelPendingOrder() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));

        // when
        order.cancel();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("STOCK_RESERVED 상태 주문은 CANCELLED로 변경할 수 있다")
    void cancelStockReservedOrder() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.reserveStock();

        // when
        order.cancel();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("CONFIRMED 상태 주문은 CANCELLED로 변경할 수 있다")
    void cancelConfirmedOrder() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.reserveStock();
        order.confirm();

        // when
        order.cancel();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("PENDING 상태 주문은 FAILED로 변경할 수 있다")
    void failPendingOrder() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));

        // when
        order.fail();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    @DisplayName("STOCK_RESERVED 상태 주문은 FAILED로 변경할 수 있다")
    void failStockReservedOrder() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.reserveStock();

        // when
        order.fail();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    @DisplayName("CONFIRMED 상태 주문은 COMPLETED로 변경할 수 있다")
    void completeConfirmedOrder() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.reserveStock();
        order.confirm();

        // when
        order.complete();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("CONFIRMED 상태 주문은 FAILED로 변경할 수 없다")
    void failConfirmedOrderThrowsException() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.reserveStock();
        order.confirm();

        // when & then
        assertThatThrownBy(order::fail)
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("CANCELLED 상태 주문은 다른 상태로 변경할 수 없다")
    void changeCancelledOrderStatusThrowsException() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.cancel();

        // when & then
        assertThatThrownBy(order::confirm)
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(order::complete)
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(order::fail)
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("FAILED 상태 주문은 다른 상태로 변경할 수 없다")
    void changeFailedOrderStatusThrowsException() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.fail();

        // when & then
        assertThatThrownBy(order::confirm)
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(order::complete)
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("COMPLETED 상태 주문은 다른 상태로 변경할 수 없다")
    void changeCompletedOrderStatusThrowsException() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));
        order.reserveStock();
        order.confirm();
        order.complete();

        // when & then
        assertThatThrownBy(order::confirm)
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThatThrownBy(order::fail)
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("주문 생성 후 5분 이내면 취소 가능 시간 검증을 통과한다")
    void validateCancelableTimeWithinLimit() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));

        // when & then
        order.validateCancelableTime(order.getOrderedAt().plusMinutes(5), Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("주문 생성 후 5분이 지나면 취소 가능 시간 검증에 실패한다")
    void validateCancelableTimeExpiredThrowsException() {
        // given
        Order order = createOrder(List.of(createOrderItem(10_000L, 1)));

        // when & then
        assertThatThrownBy(() -> order.validateCancelableTime(
                order.getOrderedAt().plusMinutes(5).plusSeconds(1),
                Duration.ofMinutes(5)
        )).isInstanceOf(OrderCancelTimeExpiredException.class);
    }

    @Test
    @DisplayName("주문 삭제 시 주문과 주문상품은 논리 삭제된다")
    void deleteOrderAndOrderItems() {
        // given
        Order order = createOrder(List.of(
                createOrderItem(10_000L, 1),
                createOrderItem(20_000L, 2)
        ));
        String userId = UUID.randomUUID().toString();

        // when
        order.delete(userId);

        // then
        assertThat(order.getDeletedAt()).isNotNull();
        assertThat(order.getDeletedBy()).isEqualTo(userId);
        assertThat(order.getOrderItems())
                .allSatisfy(item -> {
                    assertThat(item.getDeletedAt()).isNotNull();
                    assertThat(item.getDeletedBy()).isEqualTo(userId);
                });
    }

    private Order createOrder(List<OrderItem> orderItems) {
        return Order.create(
                UUID.randomUUID(),
                "ORD-001",
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                "문 앞에 놓아주세요",
                LocalDateTime.now().plusDays(3),
                UUID.randomUUID().toString(),
                orderItems
        );
    }

    private OrderItem createOrderItem(Long unitPrice, Integer quantity) {
        return OrderItem.create(
                UUID.randomUUID(),
                "상품명",
                "옵션명",
                unitPrice,
                quantity
        );
    }
}
