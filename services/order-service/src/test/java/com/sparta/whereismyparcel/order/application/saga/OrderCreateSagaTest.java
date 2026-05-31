package com.sparta.whereismyparcel.order.application.saga;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.service.OrderCreationStateService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class OrderCreateSagaTest {

    @Mock
    private CompanyFeignClient companyFeignClient;

    @Mock
    private ShipmentFeignClient shipmentFeignClient;

    @Mock
    private OrderCreationStateService orderCreationStateService;

    @InjectMocks
    private OrderCreateSaga orderCreateSaga;

    private OrderCreateSagaContext context;
    private UUID productVariantId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        productVariantId = UUID.randomUUID();
        context = createContext();
        context.applyOrderId(orderId);
    }

    @Test
    @DisplayName("재고 예약과 배송 생성이 모두 성공하면 STOCK_RESERVED, CONFIRMED 순으로 상태가 저장된다")
    void executeSuccess() {
        // given
        given(companyFeignClient.reserveStock(any(), any()))
                .willReturn(ApiResponse.success(List.of(new StockReservationResponse(productVariantId, 10))));
        given(shipmentFeignClient.createShipments(any(), any()))
                .willReturn(ApiResponse.success(List.of(createShipmentCreateResponse())));

        // when
        orderCreateSaga.execute(context);

        // then
        then(orderCreationStateService).should().markStockReserved(orderId);
        then(orderCreationStateService).should().markConfirmed(orderId);
        then(orderCreationStateService).should(never()).markFailed(any());
        then(orderCreationStateService).should(never()).markCompensationFailed(any());
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
    }

    @Test
    @DisplayName("재고 예약에 실패하면 FAILED 상태가 저장되고 SagaFailedException이 발생한다")
    void executeFailOnStockReservation() {
        // given
        given(companyFeignClient.reserveStock(any(), any()))
                .willThrow(new RuntimeException("재고 예약 실패"));

        // when & then
        assertThatThrownBy(() -> orderCreateSaga.execute(context))
                .isInstanceOf(SagaFailedException.class);

        then(orderCreationStateService).should().markFailed(orderId);
        then(orderCreationStateService).should(never()).markStockReserved(any());
        then(orderCreationStateService).should(never()).markConfirmed(any());
        then(shipmentFeignClient).should(never()).createShipments(any(), any());
    }

    @Test
    @DisplayName("배송 생성에 실패하면 재고 원복 후 FAILED 상태가 저장되고 SagaFailedException이 발생한다")
    void executeFailOnShipmentCreation() {
        // given
        given(companyFeignClient.reserveStock(any(), any()))
                .willReturn(ApiResponse.success(List.of(new StockReservationResponse(productVariantId, 10))));
        given(shipmentFeignClient.createShipments(any(), any()))
                .willThrow(new RuntimeException("배송 생성 실패"));
        given(companyFeignClient.cancelReservation(any(), any()))
                .willReturn(ApiResponse.ok());

        // when & then
        assertThatThrownBy(() -> orderCreateSaga.execute(context))
                .isInstanceOf(SagaFailedException.class);

        then(orderCreationStateService).should().markStockReserved(orderId);
        then(orderCreationStateService).should().markFailed(orderId);
        then(orderCreationStateService).should(never()).markConfirmed(any());
        then(orderCreationStateService).should(never()).markCompensationFailed(any());
        then(companyFeignClient).should(times(1)).cancelReservation(any(), any());
    }

    @Test
    @DisplayName("배송 생성 실패 후 재고 원복도 실패하면 COMPENSATION_FAILED 상태가 저장되고 SagaCompensationFailedException이 발생한다")
    void executeFailOnCompensation() {
        // given
        given(companyFeignClient.reserveStock(any(), any()))
                .willReturn(ApiResponse.success(List.of(new StockReservationResponse(productVariantId, 10))));
        given(shipmentFeignClient.createShipments(any(), any()))
                .willThrow(new RuntimeException("배송 생성 실패"));
        given(companyFeignClient.cancelReservation(any(), any()))
                .willThrow(new RuntimeException("재고 원복 실패"));

        // when & then
        assertThatThrownBy(() -> orderCreateSaga.execute(context))
                .isInstanceOf(SagaCompensationFailedException.class);

        then(orderCreationStateService).should().markStockReserved(orderId);
        then(orderCreationStateService).should().markCompensationFailed(orderId);
        then(orderCreationStateService).should(never()).markConfirmed(any());
        then(orderCreationStateService).should(never()).markFailed(any());
    }

    private OrderCreateSagaContext createContext() {
        return new OrderCreateSagaContext(
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                "ORD-20260521-ABCD1234",
                "홍길동",
                "010-1234-5678",
                "12345",
                "서울시 강남구",
                "101동 1001호",
                "문 앞에 놓아주세요",
                LocalDateTime.now().plusDays(3),
                List.of(new OrderCreateSagaContext.OrderItemInfo(
                        productVariantId, "SKU-001", "상품명", 10_000L, 2
                ))
        );
    }

    private ShipmentCreateResponse createShipmentCreateResponse() {
        return new ShipmentCreateResponse(
                UUID.randomUUID(),
                orderId,
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
