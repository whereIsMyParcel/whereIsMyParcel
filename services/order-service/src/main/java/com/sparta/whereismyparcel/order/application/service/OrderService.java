package com.sparta.whereismyparcel.order.application.service;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSaga;
import com.sparta.whereismyparcel.order.application.saga.OrderCreateSagaContext;
import com.sparta.whereismyparcel.order.domain.entity.Order;
import com.sparta.whereismyparcel.order.domain.entity.OrderItem;
import com.sparta.whereismyparcel.order.domain.entity.OrderStatus;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderItemsException;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderSearchDateRangeException;
import com.sparta.whereismyparcel.order.domain.exception.InvalidOrderStatusException;
import com.sparta.whereismyparcel.order.domain.exception.OrderNotFoundException;
import com.sparta.whereismyparcel.order.domain.exception.SagaCompensationFailedException;
import com.sparta.whereismyparcel.order.domain.exception.SagaFailedException;
import com.sparta.whereismyparcel.order.domain.repository.OrderRepository;
import com.sparta.whereismyparcel.order.infrastructure.client.CompanyFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.ShipmentCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.request.StockCancelRequest;
import com.sparta.whereismyparcel.order.infrastructure.client.dto.response.SkuValidationResponse;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderCreateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.request.OrderUpdateRequest;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCancelResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderAiContextResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCompleteResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderCreateResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderDetailResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderListResponse;
import com.sparta.whereismyparcel.order.presentation.dto.response.OrderUpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private static final String MASTER_ROLE = "MASTER";

    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Duration ORDER_CANCEL_LIMIT = Duration.ofMinutes(5);

    private final OrderRepository orderRepository;
    private final CompanyFeignClient companyFeignClient;
    private final ShipmentFeignClient shipmentFeignClient;
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
                            .filter(v -> v.id().equals(i.productVariantId()))
                            .findFirst()
                            .orElseThrow(InvalidOrderItemsException::new);
                    return OrderItem.create(
                            i.productVariantId(),
                            skuInfo.skuCode(),
                            skuInfo.variantName(),
                            skuInfo.variantName(),
                            skuInfo.variantPrice().longValue(),
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
                request.requestedDeliveryAt(),
                userId,
                orderItems
        );

        orderRepository.save(order);

        List<OrderCreateSagaContext.OrderItemInfo> itemInfos = orderItems.stream()
                .map(item -> new OrderCreateSagaContext.OrderItemInfo(
                        item.getProductVariantId(),
                        item.getSkuCode(),
                        item.getQuantity()
                ))
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

    public Page<OrderListResponse> getOrders(
            String userId,
            String role,
            OrderStatus status,
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        validateSearchDateRange(startDate, endDate);

        return orderRepository.searchOrders(
                userId,
                isMaster(role),
                status,
                keyword,
                startDate,
                endDate,
                pageable
        ).map(OrderListResponse::from);
    }

    public OrderDetailResponse getOrder(String userId, String role, UUID orderId) {
        Order order = orderRepository.findDetailByOrderId(orderId, userId, isMaster(role))
                .orElseThrow(OrderNotFoundException::new);

        return OrderDetailResponse.from(order);
    }

    public OrderAiContextResponse getOrderAiContext(UUID orderId) {
        Order order = orderRepository.findWithOrderItemsByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(OrderNotFoundException::new);

        return OrderAiContextResponse.from(order);
    }

    @Transactional
    public OrderUpdateResponse updateOrder(
            String userId,
            String role,
            UUID orderId,
            OrderUpdateRequest request
    ) {
        Order order = orderRepository.findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!isMaster(role)) {
            validateOrderOwner(order, userId);
        }

        order.updateRequestInfo(request.requestMemo(), request.requestedDeliveryAt());

        return OrderUpdateResponse.from(order);
    }

    @Transactional
    public OrderCancelResponse cancelOrder(String userId, UUID orderId) {
        Order order = orderRepository.findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(OrderNotFoundException::new);

        validateOrderOwner(order, userId);

        if (!isCancelableStatus(order.getOrderStatus())) {
            throw new InvalidOrderStatusException();
        }

        order.validateCancelableTime(LocalDateTime.now(), ORDER_CANCEL_LIMIT);

        switch (order.getOrderStatus()) {
            case PENDING -> order.cancel();
            case STOCK_RESERVED -> {
                cancelStockReservation(userId, order);
                order.cancel();
            }
            case CONFIRMED -> {
                cancelShipments(userId, order);
                cancelStockReservation(userId, order);
                order.cancel();
            }
            default -> throw new InvalidOrderStatusException();
        }

        return OrderCancelResponse.from(order);
    }

    @Transactional
    public OrderCompleteResponse completeOrder(UUID orderId) {
        Order order = orderRepository.findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            return OrderCompleteResponse.from(order);
        }

        order.complete();

        return OrderCompleteResponse.from(order);
    }

    @Transactional
    public void deleteOrder(String userId, String role, UUID orderId) {
        if (!isMaster(role)) {
            throw new OrderNotFoundException();
        }

        Order order = orderRepository.findByOrderIdAndDeletedAtIsNull(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.isDeletable()) {
            throw new InvalidOrderStatusException();
        }

        order.delete(userId);
    }

    private void validateOrderOwner(Order order, String userId) {
        if (!order.getOrderedBy().equals(userId)) {
            throw new OrderNotFoundException();
        }
    }

    private boolean isCancelableStatus(OrderStatus orderStatus) {
        return orderStatus == OrderStatus.PENDING
                || orderStatus == OrderStatus.STOCK_RESERVED
                || orderStatus == OrderStatus.CONFIRMED;
    }

    private boolean isMaster(String role) {
        return MASTER_ROLE.equals(role);
    }

    private void validateSearchDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidOrderSearchDateRangeException();
        }
    }

    private void cancelStockReservation(String userId, Order order) {
        StockCancelRequest request = new StockCancelRequest(
                order.getOrderId(),
                order.getOrderItems().stream()
                        .map(item -> new StockCancelRequest.Item(
                                item.getSkuCode(),
                                item.getQuantity()
                        ))
                        .toList()
        );

        ApiResponse<Void> response = companyFeignClient.cancelReservation(userId, request);
        ensureCompensationSuccess(response);
    }

    private void cancelShipments(String userId, Order order) {
        ShipmentCancelRequest request = new ShipmentCancelRequest(order.getOrderId());
        ApiResponse<Void> response = shipmentFeignClient.cancelShipments(userId, request);
        ensureCompensationSuccess(response);
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

    private void ensureCompensationSuccess(ApiResponse<Void> response) {
        if (response == null || !response.success()) {
            throw new SagaCompensationFailedException();
        }
    }
}
