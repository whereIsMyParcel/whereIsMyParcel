package com.sparta.whereismyparcel.order.application.service;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSaga;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.domain.repository.OrderRepository;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.SkuValidationResponse;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCreateResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CompanyFeignClient companyFeignClient;

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
            Order o = invocation.getArgument(0);
            o.confirm();
            return null;
        }).given(orderCreateSaga).execute(any(), any());

        // when
        OrderCreateResponse response = orderService.createOrder(userId, request);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("상품 검증에 실패하면 예외가 발생하고 주문이 생성되지 않는다")
    void createOrderFailOnValidation() {
        // given
        String userId = UUID.randomUUID().toString();
        OrderCreateRequest request = createRequest();
        given(companyFeignClient.validateProducts(any(), any()))
                .willThrow(new RuntimeException("상품 검증 실패"));

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> orderService.createOrder(userId, request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Saga 실패 시 주문이 FAILED 상태로 저장된다")
    void createOrderFailOnSaga() {
        // given
        String userId = UUID.randomUUID().toString();
        OrderCreateRequest request = createRequest();
        given(companyFeignClient.validateProducts(any(), any()))
                .willReturn(ApiResponse.success(createValidationResponse(request)));
        given(orderRepository.save(any())).willAnswer(i -> i.getArgument(0));
        willAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.fail();
            throw new SagaFailedException();
        }).given(orderCreateSaga).execute(any(), any());

        // when
        OrderCreateResponse response = orderService.createOrder(userId, request);

        // then
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.FAILED);
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
}
