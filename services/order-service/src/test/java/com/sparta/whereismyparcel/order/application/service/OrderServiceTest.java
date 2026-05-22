package com.sparta.whereismyparcel.order.application.service;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSaga;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderStatusException;
import com.sparta.whereismyparcel.order.domain.exception.OrderErrorCode;
import com.sparta.whereismyparcel.order.domain.exception.SagaCompensationFailedException;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.domain.repository.OrderRepository;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.SkuValidationResponse;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCancelResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCreateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CompanyFeignClient companyFeignClient;

    @Mock
    private ShipmentFeignClient shipmentFeignClient;

    @Mock
    private OrderCreateSaga orderCreateSaga;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성에 성공하면 CONFIRMED 상태의 응답을 반환한다")
    void createOrderSuccess() {
        // given
        String userId = UUID.randomUUID().toString();
        OrderCreateRequest request = createRequest();
        given(companyFeignClient.validateProducts(any(), any()))
                .willReturn(ApiResponse.success(createValidationResponse(request)));
        given(orderRepository.save(any())).willAnswer(i -> i.getArgument(0));
        willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.reserveStock();
            order.confirm();
            return null;
        }).given(orderCreateSaga).execute(any(), any());

        // when
        OrderCreateResponse response = orderService.createOrder(userId, request);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("상품 검증에 실패하면 예외가 발생하고 주문은 생성되지 않는다")
    void createOrderFailOnValidation() {
        // given
        String userId = UUID.randomUUID().toString();
        OrderCreateRequest request = createRequest();
        given(companyFeignClient.validateProducts(any(), any()))
                .willThrow(new RuntimeException("상품 검증 실패"));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Saga 실패 시 주문은 FAILED 상태로 저장된다")
    void createOrderFailOnSaga() {
        // given
        String userId = UUID.randomUUID().toString();
        OrderCreateRequest request = createRequest();
        given(companyFeignClient.validateProducts(any(), any()))
                .willReturn(ApiResponse.success(createValidationResponse(request)));
        given(orderRepository.save(any())).willAnswer(i -> i.getArgument(0));
        willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.fail();
            throw new SagaFailedException();
        }).given(orderCreateSaga).execute(any(), any());

        // when
        OrderCreateResponse response = orderService.createOrder(userId, request);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    @DisplayName("PENDING 상태 주문은 외부 보상 없이 취소된다")
    void cancelPendingOrder() {
        // given
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder();
        given(orderRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(order));

        // when
        OrderCancelResponse response = orderService.cancelOrder(userId, orderId);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
        then(shipmentFeignClient).should(never()).cancelShipments(any(), any());
    }

    @Test
    @DisplayName("STOCK_RESERVED 상태 주문은 재고 원복 후 취소된다")
    void cancelStockReservedOrder() {
        // given
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder();
        order.reserveStock();
        given(orderRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(order));
        given(companyFeignClient.cancelReservation(any(), any()))
                .willReturn(ApiResponse.ok());

        // when
        OrderCancelResponse response = orderService.cancelOrder(userId, orderId);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(companyFeignClient).should().cancelReservation(any(), any());
        then(shipmentFeignClient).should(never()).cancelShipments(any(), any());
    }

    @Test
    @DisplayName("CONFIRMED 상태 주문은 배송 취소와 재고 원복 후 취소된다")
    void cancelConfirmedOrder() {
        // given
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder();
        order.reserveStock();
        order.confirm();
        given(orderRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(order));
        given(shipmentFeignClient.cancelShipments(any(), any()))
                .willReturn(ApiResponse.ok());
        given(companyFeignClient.cancelReservation(any(), any()))
                .willReturn(ApiResponse.ok());

        // when
        OrderCancelResponse response = orderService.cancelOrder(userId, orderId);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(shipmentFeignClient).should().cancelShipments(any(), any());
        then(companyFeignClient).should().cancelReservation(any(), any());
    }

    @Test
    @DisplayName("재고 원복에 실패하면 주문은 취소되지 않는다")
    void cancelStockReservedOrderFailOnStockCompensation() {
        // given
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder();
        order.reserveStock();
        given(orderRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(order));
        given(companyFeignClient.cancelReservation(any(), any()))
                .willReturn(ApiResponse.error(OrderErrorCode.SAGA_COMPENSATION_FAILED));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(userId, orderId))
                .isInstanceOf(SagaCompensationFailedException.class);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
    }

    @Test
    @DisplayName("배송 취소에 실패하면 재고 원복을 요청하지 않고 주문은 취소되지 않는다")
    void cancelConfirmedOrderFailOnShipmentCompensation() {
        // given
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder();
        order.reserveStock();
        order.confirm();
        given(orderRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(order));
        given(shipmentFeignClient.cancelShipments(any(), any()))
                .willReturn(ApiResponse.error(OrderErrorCode.SAGA_COMPENSATION_FAILED));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(userId, orderId))
                .isInstanceOf(SagaCompensationFailedException.class);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
    }

    @Test
    @DisplayName("취소할 수 없는 상태의 주문은 예외가 발생한다")
    void cancelInvalidStatusOrderThrowsException() {
        // given
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder();
        order.fail();
        given(orderRepository.findByOrderIdAndDeletedAtIsNull(orderId))
                .willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(userId, orderId))
                .isInstanceOf(InvalidOrderStatusException.class);
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
        then(shipmentFeignClient).should(never()).cancelShipments(any(), any());
    }

    private OrderCreateRequest createRequest() {
        return new OrderCreateRequest(
                UUID.randomUUID(),
                "문 앞에 놓아주세요",
                LocalDateTime.now().plusDays(3),
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                List.of(new OrderCreateRequest.OrderItemCreateRequest(UUID.randomUUID(), 2))
        );
    }

    private SkuValidationResponse createValidationResponse(OrderCreateRequest request) {
        List<SkuValidationResponse.Item> items = request.items().stream()
                .map(i -> new SkuValidationResponse.Item(
                        i.productVariantId(), "상품명", "옵션명", 10_000L))
                .toList();
        return new SkuValidationResponse(items);
    }

    private Order createOrder() {
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
                List.of(OrderItem.create(
                        UUID.randomUUID(),
                        "상품명",
                        "옵션명",
                        10_000L,
                        2
                ))
        );
    }
}
