package com.sparta.whereismyparcel.order.application.service;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSaga;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.OrderStatus;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderSearchDateRangeException;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderStatusException;
import com.sparta.whereismyparcel.order.domain.exception.OrderErrorCode;
import com.sparta.whereismyparcel.order.domain.exception.OrderNotFoundException;
import com.sparta.whereismyparcel.order.domain.exception.SagaCompensationFailedException;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.domain.repository.OrderRepository;
import com.sparta.whereismyparcel.order.infrastructure.client.AiSlackFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.SkuValidationResponse;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderDispatchDeadlineUpdateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderUpdateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private AiSlackFeignClient aiSlackFeignClient;

    @Mock
    private OrderCreateSaga orderCreateSaga;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성에 성공하면 CONFIRMED 상태의 응답을 반환한다")
    void createOrderSuccess() {
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
        given(aiSlackFeignClient.createAiAnalysisRequest(any(), any()))
                .willReturn(ApiResponse.success(UUID.randomUUID()));

        OrderCreateResponse response = orderService.createOrder(userId, request);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        then(aiSlackFeignClient).should().createAiAnalysisRequest(any(), any());
    }

    @Test
    @DisplayName("상품 검증에 실패하면 예외가 발생하고 주문은 생성되지 않는다")
    void createOrderFailOnValidation() {
        String userId = UUID.randomUUID().toString();
        OrderCreateRequest request = createRequest();
        given(companyFeignClient.validateProducts(any(), any()))
                .willThrow(new RuntimeException("상품 검증 실패"));

        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Saga 실패 시 주문은 FAILED 상태로 저장된다")
    void createOrderFailOnSaga() {
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

        OrderCreateResponse response = orderService.createOrder(userId, request);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.FAILED);
        then(aiSlackFeignClient).should(never()).createAiAnalysisRequest(any(), any());
    }

    @Test
    @DisplayName("AI 분석 요청에 실패해도 주문 생성 성공 응답은 유지된다")
    void createOrderSuccessEvenIfAiAnalysisRequestFails() {
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
        given(aiSlackFeignClient.createAiAnalysisRequest(any(), any()))
                .willThrow(new RuntimeException("AI service unavailable"));

        OrderCreateResponse response = orderService.createOrder(userId, request);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        then(aiSlackFeignClient).should().createAiAnalysisRequest(any(), any());
    }

    @Test
    @DisplayName("PENDING 상태 주문은 외부 보상 없이 취소된다")
    void cancelPendingOrder() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderCancelResponse response = orderService.cancelOrder(userId, "COMPANY_MANAGER", orderId);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
        then(shipmentFeignClient).should(never()).cancelShipments(any(), any());
    }

    @Test
    @DisplayName("STOCK_RESERVED 상태 주문은 재고 원복 후 취소된다")
    void cancelStockReservedOrder() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.reserveStock();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(companyFeignClient.cancelReservation(any(), any())).willReturn(ApiResponse.ok());

        OrderCancelResponse response = orderService.cancelOrder(userId, "COMPANY_MANAGER", orderId);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(companyFeignClient).should().cancelReservation(any(), any());
        then(shipmentFeignClient).should(never()).cancelShipments(any(), any());
    }

    @Test
    @DisplayName("CONFIRMED 상태 주문은 배송 취소와 재고 원복 후 취소된다")
    void cancelConfirmedOrder() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.reserveStock();
        order.confirm();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(shipmentFeignClient.cancelShipments(any(), any())).willReturn(ApiResponse.ok());
        given(companyFeignClient.cancelReservation(any(), any())).willReturn(ApiResponse.ok());

        OrderCancelResponse response = orderService.cancelOrder(userId, "COMPANY_MANAGER", orderId);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(shipmentFeignClient).should().cancelShipments(any(), any());
        then(companyFeignClient).should().cancelReservation(any(), any());
    }

    @Test
    @DisplayName("재고 원복에 실패하면 주문은 취소되지 않는다")
    void cancelStockReservedOrderFailOnStockCompensation() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.reserveStock();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(companyFeignClient.cancelReservation(any(), any()))
                .willReturn(ApiResponse.error(OrderErrorCode.SAGA_COMPENSATION_FAILED));

        assertThatThrownBy(() -> orderService.cancelOrder(userId, "COMPANY_MANAGER", orderId))
                .isInstanceOf(SagaCompensationFailedException.class);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
    }

    @Test
    @DisplayName("배송 취소에 실패하면 재고 원복을 요청하지 않고 주문은 취소되지 않는다")
    void cancelConfirmedOrderFailOnShipmentCompensation() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.reserveStock();
        order.confirm();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(shipmentFeignClient.cancelShipments(any(), any()))
                .willReturn(ApiResponse.error(OrderErrorCode.SAGA_COMPENSATION_FAILED));

        assertThatThrownBy(() -> orderService.cancelOrder(userId, "COMPANY_MANAGER", orderId))
                .isInstanceOf(SagaCompensationFailedException.class);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
    }

    @Test
    @DisplayName("취소할 수 없는 상태의 주문은 예외가 발생한다")
    void cancelInvalidStatusOrderThrowsException() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.fail();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(userId, "COMPANY_MANAGER", orderId))
                .isInstanceOf(InvalidOrderStatusException.class);
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
        then(shipmentFeignClient).should(never()).cancelShipments(any(), any());
    }

    @Test
    @DisplayName("주문자가 아닌 사용자는 주문을 취소할 수 없다")
    void cancelOrderByNonOwnerThrowsException() {
        String ownerId = UUID.randomUUID().toString();
        String otherUserId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(ownerId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(otherUserId, "COMPANY_MANAGER", orderId))
                .isInstanceOf(OrderNotFoundException.class);
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
        then(shipmentFeignClient).should(never()).cancelShipments(any(), any());
    }

    @Test
    @DisplayName("MASTER는 다른 사용자의 PENDING 주문을 취소할 수 있다")
    void cancelPendingOrderByMaster() {
        String masterId = UUID.randomUUID().toString();
        String ownerId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(ownerId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderCancelResponse response = orderService.cancelOrder(masterId, "MASTER", orderId);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(companyFeignClient).should(never()).cancelReservation(any(), any());
        then(shipmentFeignClient).should(never()).cancelShipments(any(), any());
    }

    @Test
    @DisplayName("MASTER가 STOCK_RESERVED 주문 취소 시 실제 주문 소유자 ID로 재고를 원복한다")
    void cancelStockReservedOrderByMasterUsesOwnerId() {
        // given
        String masterId = UUID.randomUUID().toString();
        String ownerId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(ownerId);
        order.reserveStock();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(companyFeignClient.cancelReservation(any(), any())).willReturn(ApiResponse.ok());

        // when
        OrderCancelResponse response = orderService.cancelOrder(masterId, "MASTER", orderId);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(companyFeignClient).should().cancelReservation(eq(ownerId), any());
    }

    @Test
    @DisplayName("MASTER가 CONFIRMED 주문 취소 시 실제 주문 소유자 ID로 배송 취소와 재고를 원복한다")
    void cancelConfirmedOrderByMasterUsesOwnerId() {
        // given
        String masterId = UUID.randomUUID().toString();
        String ownerId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(ownerId);
        order.reserveStock();
        order.confirm();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(shipmentFeignClient.cancelShipments(any(), any())).willReturn(ApiResponse.ok());
        given(companyFeignClient.cancelReservation(any(), any())).willReturn(ApiResponse.ok());

        // when
        OrderCancelResponse response = orderService.cancelOrder(masterId, "MASTER", orderId);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        then(shipmentFeignClient).should().cancelShipments(eq(ownerId), any());
        then(companyFeignClient).should().cancelReservation(eq(ownerId), any());
    }

    @Test
    @DisplayName("CONFIRMED 상태 주문은 완료 처리할 수 있다")
    void completeConfirmedOrder() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.reserveStock();
        order.confirm();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        orderService.completeOrder(orderId);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("이미 완료된 주문 완료 요청은 성공 응답을 반환한다")
    void completeAlreadyCompletedOrderReturnsSuccess() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.reserveStock();
        order.confirm();
        order.complete();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        orderService.completeOrder(orderId);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 완료 처리할 수 없다")
    void completeOrderNotFoundThrowsException() {
        UUID orderId = UUID.randomUUID();
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.completeOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("CONFIRMED가 아닌 주문은 완료 처리할 수 없다")
    void completeInvalidStatusOrderThrowsException() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.completeOrder(orderId))
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("검색 시작일이 종료일보다 이후이면 주문 목록을 조회할 수 없다")
    void getOrdersWithInvalidDateRangeThrowsException() {
        String userId = UUID.randomUUID().toString();
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime endDate = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        assertThatThrownBy(() -> orderService.getOrders(
                userId, "MASTER", null, null, startDate, endDate, pageable
        )).isInstanceOf(InvalidOrderSearchDateRangeException.class);
    }

    @Test
    @DisplayName("주문자는 PENDING 상태 주문의 요청 정보를 수정할 수 있다")
    void updatePendingOrderByOwner() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        OrderUpdateRequest request = new OrderUpdateRequest("변경 요청사항", LocalDateTime.now().plusDays(5));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderUpdateResponse response = orderService.updateOrder(userId, "COMPANY_MANAGER", orderId, request);

        assertThat(response.requestMemo()).isEqualTo(request.requestMemo());
        assertThat(response.requestedDeliveryAt()).isEqualTo(request.requestedDeliveryAt());
    }

    @Test
    @DisplayName("MASTER는 STOCK_RESERVED 상태 주문의 요청 정보를 수정할 수 있다")
    void updateStockReservedOrderByMaster() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(UUID.randomUUID().toString());
        order.reserveStock();
        OrderUpdateRequest request = new OrderUpdateRequest("변경 요청사항", LocalDateTime.now().plusDays(5));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderUpdateResponse response = orderService.updateOrder(userId, "MASTER", orderId, request);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        assertThat(response.requestMemo()).isEqualTo(request.requestMemo());
    }

    @Test
    @DisplayName("요청 정보 일부만 수정하면 나머지 값은 유지된다")
    void updateOrderPartiallyKeepsExistingValue() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        LocalDateTime originalRequestedDeliveryAt = order.getRequestedDeliveryAt();
        OrderUpdateRequest request = new OrderUpdateRequest("변경 요청사항", null);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderUpdateResponse response = orderService.updateOrder(userId, "COMPANY_MANAGER", orderId, request);

        assertThat(response.requestMemo()).isEqualTo(request.requestMemo());
        assertThat(response.requestedDeliveryAt()).isEqualTo(originalRequestedDeliveryAt);
    }

    @Test
    @DisplayName("주문자가 아닌 사용자는 주문 요청 정보를 수정할 수 없다")
    void updateOrderByNonOwnerThrowsException() {
        String ownerId = UUID.randomUUID().toString();
        String otherUserId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(ownerId);
        OrderUpdateRequest request = new OrderUpdateRequest("변경 요청사항", LocalDateTime.now().plusDays(5));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrder(otherUserId, "COMPANY_MANAGER", orderId, request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("CONFIRMED 상태 주문은 요청 정보를 수정할 수 없다")
    void updateConfirmedOrderThrowsException() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.reserveStock();
        order.confirm();
        OrderUpdateRequest request = new OrderUpdateRequest("변경 요청사항", LocalDateTime.now().plusDays(5));
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrder(userId, "COMPANY_MANAGER", orderId, request))
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 요청 정보를 수정할 수 없다")
    void updateOrderNotFoundThrowsException() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        OrderUpdateRequest request = new OrderUpdateRequest("변경 요청사항", LocalDateTime.now().plusDays(5));
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(userId, "MASTER", orderId, request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("MASTER는 주문을 삭제할 수 있다")
    void deleteOrderByMaster() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        order.fail();
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        orderService.deleteOrder(userId, "MASTER", orderId);

        assertThat(order.getDeletedAt()).isNotNull();
        assertThat(order.getDeletedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("PENDING 상태 주문은 삭제할 수 없다")
    void deletePendingOrderThrowsException() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deleteOrder(userId, "MASTER", orderId))
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThat(order.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("MASTER가 아닌 사용자는 주문을 삭제할 수 없다")
    void deleteOrderByNonMasterThrowsException() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();

        assertThatThrownBy(() -> orderService.deleteOrder(userId, "COMPANY_MANAGER", orderId))
                .isInstanceOf(OrderNotFoundException.class);
        then(orderRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 주문은 삭제할 수 없다")
    void deleteOrderNotFoundThrowsException() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(userId, "MASTER", orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("MASTER는 전체 주문 목록을 조회할 수 있다")
    void getOrdersByMaster() {
        String userId = UUID.randomUUID().toString();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Order order = createOrder(userId);
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        given(orderRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Order>>any(),
                eq(pageable)
        )).willReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<OrderListResponse> response = orderService.getOrders(
                userId, "MASTER", OrderStatus.PENDING, "ORD", startDate, endDate, pageable
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).orderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("MASTER가 아닌 사용자는 본인 주문 목록만 조회한다")
    void getOrdersByNonMaster() {
        String userId = UUID.randomUUID().toString();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Order order = createOrder(userId);

        given(orderRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Order>>any(),
                eq(pageable)
        )).willReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<OrderListResponse> response = orderService.getOrders(
                userId, "COMPANY_MANAGER", null, null, null, null, pageable
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).orderNumber()).isEqualTo(order.getOrderNumber());
    }

    @Test
    @DisplayName("MASTER는 주문 단건 상세를 조회할 수 있다")
    void getOrderByMaster() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);

        given(orderRepository.findDetailByOrderId(orderId, userId, true))
                .willReturn(Optional.of(order));

        OrderDetailResponse response = orderService.getOrder(userId, "MASTER", orderId);

        assertThat(response.orderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("MASTER가 아닌 사용자는 본인 주문 단건 상세만 조회할 수 있다")
    void getOrderByNonMaster() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(userId);

        given(orderRepository.findDetailByOrderId(orderId, userId, false))
                .willReturn(Optional.of(order));

        OrderDetailResponse response = orderService.getOrder(userId, "COMPANY_MANAGER", orderId);

        assertThat(response.orderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(response.orderedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("조회 권한이 없는 주문 단건 상세는 조회할 수 없다")
    void getOrderWithoutPermissionThrowsException() {
        String userId = UUID.randomUUID().toString();
        UUID orderId = UUID.randomUUID();

        given(orderRepository.findDetailByOrderId(orderId, userId, false))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(userId, "COMPANY_MANAGER", orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("AI 프롬프트용 주문 컨텍스트를 조회할 수 있다")
    void getOrderAiContext() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(UUID.randomUUID().toString());

        given(orderRepository.findWithOrderItemsByOrderId(orderId))
                .willReturn(Optional.of(order));

        OrderAiContextResponse response = orderService.getOrderAiContext(orderId);

        assertThat(response.orderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(response.recipientName()).isEqualTo(order.getRecipientName());
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 AI 프롬프트용 컨텍스트를 조회할 수 없다")
    void getOrderAiContextNotFoundThrowsException() {
        UUID orderId = UUID.randomUUID();

        given(orderRepository.findWithOrderItemsByOrderId(orderId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderAiContext(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("AI가 계산한 최종 출고 상한을 주문에 반영할 수 있다")
    void updateFinalDispatchDeadline() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(UUID.randomUUID().toString());
        order.reserveStock();
        order.confirm();
        LocalDateTime finalDispatchDeadline = LocalDateTime.now().plusDays(1);
        OrderDispatchDeadlineUpdateRequest request =
                new OrderDispatchDeadlineUpdateRequest(finalDispatchDeadline);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderDispatchDeadlineUpdateResponse response =
                orderService.updateFinalDispatchDeadline(orderId, request);

        assertThat(response.finalDispatchDeadline()).isEqualTo(finalDispatchDeadline);
        assertThat(order.getFinalDispatchDeadline()).isEqualTo(finalDispatchDeadline);
    }

    @Test
    @DisplayName("존재하지 않는 주문에는 최종 출고 상한을 반영할 수 없다")
    void updateFinalDispatchDeadlineNotFoundThrowsException() {
        UUID orderId = UUID.randomUUID();
        OrderDispatchDeadlineUpdateRequest request =
                new OrderDispatchDeadlineUpdateRequest(LocalDateTime.now().plusDays(1));

        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateFinalDispatchDeadline(orderId, request))
                .isInstanceOf(OrderNotFoundException.class);
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

    private List<SkuValidationResponse> createValidationResponse(OrderCreateRequest request) {
        return request.items().stream()
                .map(i -> new SkuValidationResponse(
                        i.productVariantId(), "SKU-001", "옵션명", 10_000, "ON_SALE"))
                .toList();
    }

    private Order createOrder(String orderedBy) {
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
                orderedBy,
                List.of(OrderItem.create(
                        UUID.randomUUID(),
                        "SKU-001",
                        "상품명",
                        "옵션명",
                        10_000L,
                        2
                ))
        );
    }
}
