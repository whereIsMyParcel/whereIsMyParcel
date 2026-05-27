package com.sparta.whereismyparcel.order.application.saga;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;
import com.sparta.whereismyparcel.order.domain.exception.SagaCompensationFailedException;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.ShipmentCreateResponse;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.StockReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OrderCreateSagaTest {

    @Mock
    private CompanyFeignClient companyFeignClient;

    @Mock
    private ShipmentFeignClient shipmentFeignClient;

    @InjectMocks
    private OrderCreateSaga orderCreateSaga;

    private Order order;
    private OrderCreateSagaContext context;
    private UUID productVariantId;

    @BeforeEach
    void setUp() {
        order = createOrder();
        productVariantId = UUID.randomUUID();
        context = new OrderCreateSagaContext(
                order.getOrderId(),
                UUID.randomUUID().toString(),
                List.of(new OrderCreateSagaContext.OrderItemInfo(productVariantId, "SKU-001", 10))
        );
    }

    @Test
    @DisplayName("재고 예약과 배송 생성이 모두 성공하면 주문이 CONFIRMED 상태가 된다")
    void executeSuccess() {
        // given
        given(companyFeignClient.reserveStock(any(), any()))
                .willReturn(ApiResponse.success(List.of(new StockReservationResponse(productVariantId, 10))));
        given(shipmentFeignClient.createShipments(any(), any()))
                .willReturn(ApiResponse.success(List.of(createShipmentCreateResponse())));

        // when
        orderCreateSaga.execute(order, context);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("재고 예약에 실패하면 주문이 FAILED 상태가 되고 SagaFailedException이 발생한다")
    void executeFailOnStockReservation() {
        // given
        given(companyFeignClient.reserveStock(any(), any()))
                .willThrow(new RuntimeException("재고 예약 실패"));

        // when & then
        assertThatThrownBy(() -> orderCreateSaga.execute(order, context))
                .isInstanceOf(SagaFailedException.class);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        then(shipmentFeignClient).should(never()).createShipments(any(), any());
    }

    @Test
    @DisplayName("배송 생성에 실패하면 재고 원복을 요청하고 주문이 FAILED 상태가 된다")
    void executeFailOnShipmentCreation() {
        // given
        given(companyFeignClient.reserveStock(any(), any()))
                .willReturn(ApiResponse.success(List.of(new StockReservationResponse(productVariantId, 10))));
        given(shipmentFeignClient.createShipments(any(), any()))
                .willThrow(new RuntimeException("배송 생성 실패"));
        given(companyFeignClient.cancelReservation(any(), any()))
                .willReturn(ApiResponse.ok());

        // when & then
        assertThatThrownBy(() -> orderCreateSaga.execute(order, context))
                .isInstanceOf(SagaFailedException.class);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        then(companyFeignClient).should(times(1)).cancelReservation(any(), any());
    }

    @Test
    @DisplayName("배송 생성 실패 후 재고 원복도 실패하면 SagaCompensationFailedException이 발생한다")
    void executeFailOnCompensation() {
        // given
        given(companyFeignClient.reserveStock(any(), any()))
                .willReturn(ApiResponse.success(List.of(new StockReservationResponse(productVariantId, 10))));
        given(shipmentFeignClient.createShipments(any(), any()))
                .willThrow(new RuntimeException("배송 생성 실패"));
        given(companyFeignClient.cancelReservation(any(), any()))
                .willThrow(new RuntimeException("재고 원복 실패"));

        // when & then
        assertThatThrownBy(() -> orderCreateSaga.execute(order, context))
                .isInstanceOf(SagaCompensationFailedException.class);
    }

    private Order createOrder() {
        return Order.create(
                UUID.randomUUID(),
                "ORD-20260521-ABCD1234",
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                "문 앞에 놓아주세요",
                LocalDateTime.now().plusDays(3),
                UUID.randomUUID().toString(),
                List.of(OrderItem.create(UUID.randomUUID(), "SKU-001", "상품명", "옵션명", 10_000L, 2))
        );
    }

    private ShipmentCreateResponse createShipmentCreateResponse() {
        return new ShipmentCreateResponse(
                UUID.randomUUID(),
                order.getOrderId(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "READY",
                "SHP-001",
                "서울시 강남구",
                "홍길동",
                "010-1234-5678",
                List.of()
        );
    }
}
