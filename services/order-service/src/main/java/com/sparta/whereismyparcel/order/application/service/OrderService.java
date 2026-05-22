package com.sparta.whereismyparcel.order.application.service;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSaga;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSagaContext;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderItemsException;
import com.sparta.whereismyparcel.order.domain.exception.SagaCompensationFailedException;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.domain.repository.OrderRepository;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.SkuValidationResponse;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final CompanyFeignClient companyFeignClient;
    private final OrderCreateSaga orderCreateSaga;

    @Transactional
    public OrderCreateResponse createOrder(String userId, OrderCreateRequest request) {
        List<UUID> productVariantIds = request.items().stream()
                .map(OrderCreateRequest.OrderItemCreateRequest::productVariantId)
                .toList();
        SkuValidationResponse validation = resolveSkuValidation(
                companyFeignClient.validateProducts(userId, productVariantIds)
        );

        List<OrderItem> orderItems = request.items().stream()
                .map(i -> {
                    SkuValidationResponse.Item skuInfo = validation.items().stream()
                            .filter(v -> v.skuId().equals(i.productVariantId()))
                            .findFirst()
                            .orElseThrow(InvalidOrderItemsException::new);
                    return OrderItem.create(
                            i.productVariantId(),
                            skuInfo.productName(),
                            skuInfo.optionName(),
                            skuInfo.unitPrice(),
                            i.quantity()
                    );
                })
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

        List<OrderCreateSagaContext.OrderItemInfo> itemInfos = request.items().stream()
                .map(i -> new OrderCreateSagaContext.OrderItemInfo(i.productVariantId(), i.quantity()))
                .toList();
        OrderCreateSagaContext context = new OrderCreateSagaContext(
                order.getOrderId(), userId, itemInfos);

        try {
            orderCreateSaga.execute(order, context);
        } catch (SagaCompensationFailedException e) {
            log.error("[OrderService] 보상 실패. orderId={}", order.getOrderId(), e);
        } catch (SagaFailedException e) {
            log.error("[OrderService] Saga 실패. orderId={}", order.getOrderId(), e);
        } finally {
            orderRepository.save(order);
        }

        return OrderCreateResponse.from(order);
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(ORDER_NUMBER_DATE_FORMAT);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return "ORD-" + date + "-" + suffix;
    }

    private SkuValidationResponse resolveSkuValidation(ApiResponse<SkuValidationResponse> response) {
        if (response == null || !response.success() || response.data() == null) {
            throw new InvalidOrderItemsException();
        }
        return response.data();
    }
}
